/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.internals.codegen.dsl.update;

import static info.archinnov.achilles.internals.parser.TypeUtils.*;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import info.archinnov.achilles.internals.codegen.dsl.AbstractDSLCodeGen;
import info.archinnov.achilles.internals.codegen.meta.EntityMetaCodeGen.EntityMetaSignature;

public class UpdateWhereDSLCodeGen extends AbstractDSLCodeGen {
    private static final MethodSpec WHERE_CONSTRUCTOR = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(UPDATE_WHERE, "where")
            .addStatement("super(where)")
            .build();


    public static List<TypeSpec> buildWhereClasses(EntityMetaSignature signature) {
        final List<FieldSignatureInfo> partitionKeys = getPartitionKeysSignatureInfo(signature.parsingResults);
        final List<FieldSignatureInfo> clusteringCols = getClusteringColsSignatureInfo(signature.parsingResults);

        final ClassSignatureParams classSignatureParams = ClassSignatureParams.of(UPDATE_DSL_SUFFIX,
                UPDATE_WHERE_DSL_SUFFIX, UPDATE_END_DSL_SUFFIX,
                ABSTRACT_UPDATE_WHERE, ABSTRACT_UPDATE_WHERE, ABSTRACT_UPDATE_END);

        final List<ClassSignatureInfo> classesSignature =
                buildClassesSignatureForWhereClause(signature, classSignatureParams, partitionKeys, clusteringCols,
                        WhereClauseFor.NORMAL);

        boolean hasCounter = hasCounter(signature);
        final ClassSignatureInfo lastSignature = classesSignature.get(classesSignature.size() - 1);

        final List<TypeSpec> partitionKeysWhereClasses = buildWhereClassesForPartitionKeys(partitionKeys, classesSignature);

        final List<TypeSpec> clusteringColsWhereClasses = buildWhereClassesForClusteringColumns(clusteringCols, classesSignature);

        final TypeSpec updateEndClass = buildUpdateEndClass(signature, lastSignature, hasCounter);

        partitionKeysWhereClasses.addAll(clusteringColsWhereClasses);
        partitionKeysWhereClasses.add(updateEndClass);
        return partitionKeysWhereClasses;
    }

    public static List<TypeSpec> buildWhereClassesForStatic(EntityMetaSignature signature) {
        final List<FieldSignatureInfo> partitionKeys = getPartitionKeysSignatureInfo(signature.parsingResults);
        final List<FieldSignatureInfo> clusteringCols = getClusteringColsSignatureInfo(signature.parsingResults);

        final ClassSignatureParams classSignatureParams = ClassSignatureParams.of(UPDATE_STATIC_DSL_SUFFIX,
                UPDATE_STATIC_WHERE_DSL_SUFFIX, UPDATE_STATIC_END_DSL_SUFFIX,
                ABSTRACT_UPDATE_WHERE, ABSTRACT_UPDATE_WHERE, ABSTRACT_UPDATE_END);

        final List<ClassSignatureInfo> classesSignature =
                buildClassesSignatureForWhereClause(signature, classSignatureParams, partitionKeys, clusteringCols,
                        WhereClauseFor.STATIC);

        boolean hasCounter = hasCounter(signature);
        final ClassSignatureInfo lastSignature = classesSignature.get(classesSignature.size() - 1);

        final List<TypeSpec> partitionKeysWhereClasses = buildWhereClassesForPartitionKeys(partitionKeys, classesSignature);

        final TypeSpec updateEndClass = buildUpdateEndClass(signature, lastSignature, hasCounter);

        partitionKeysWhereClasses.add(updateEndClass);
        return partitionKeysWhereClasses;
    }

    private static TypeSpec buildUpdateEndClass(EntityMetaSignature signature,
                                                ClassSignatureInfo lastSignature,
                                                boolean hasCounter) {

        final TypeSpec.Builder builder = TypeSpec.classBuilder(lastSignature.className)
                .superclass(lastSignature.superType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(buildWhereConstructor(UPDATE_WHERE))
                .addMethod(buildGetEntityClass(signature))
                .addMethod(buildGetMetaInternal(signature.entityRawClass))
                .addMethod(buildGetRte())
                .addMethod(buildGetOptions())
                .addMethod(buildGetBoundValuesInternal())
                .addMethod(buildGetEncodedBoundValuesInternal())
                .addMethod(buildGetThis(lastSignature.returnClassType));

        buildLWtConditionMethods(signature, lastSignature, hasCounter, builder);

        return builder.build();
    }

    private static List<TypeSpec> buildWhereClassesForPartitionKeys(List<FieldSignatureInfo> partitionKeys,
                                                                    List<ClassSignatureInfo> classesSignature) {
        if (partitionKeys.isEmpty()) {
            return new ArrayList<>();
        } else {
            final FieldSignatureInfo partitionKeyInfo = partitionKeys.get(0);
            final ClassSignatureInfo currentSignature = classesSignature.get(0);
            final ClassSignatureInfo nextSignature = classesSignature.get(1);
            partitionKeys.remove(0);
            classesSignature.remove(0);
            final TypeSpec typeSpec = buildUpdateWhereForPartitionKey(partitionKeyInfo, currentSignature, nextSignature);
            final List<TypeSpec> typeSpecs = buildWhereClassesForPartitionKeys(partitionKeys, classesSignature);
            typeSpecs.add(0, typeSpec);
            return typeSpecs;
        }
    }

    private static TypeSpec buildUpdateWhereForPartitionKey(FieldSignatureInfo partitionInfo,
                                                            ClassSignatureInfo classSignature,
                                                            ClassSignatureInfo nextSignature) {


        final TypeSpec.Builder builder = TypeSpec.classBuilder(classSignature.className)
                .superclass(classSignature.superType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(WHERE_CONSTRUCTOR)
                .addMethod(buildColumnRelation(EQ, nextSignature.returnClassType, partitionInfo))
                .addMethod(buildColumnInVarargs(nextSignature.returnClassType, partitionInfo));

        return builder.build();
    }


    private static List<TypeSpec> buildWhereClassesForClusteringColumns(List<FieldSignatureInfo> clusteringCols,
                                                                        List<ClassSignatureInfo> classesSignature) {
        if (clusteringCols.isEmpty()) {
            return new ArrayList<>();
        } else {
            final ClassSignatureInfo classSignature = classesSignature.get(0);
            final ClassSignatureInfo nextSignature = classesSignature.get(1);
            final FieldSignatureInfo clusteringColumnInfo = clusteringCols.get(0);
            clusteringCols.remove(0);
            classesSignature.remove(0);
            final TypeSpec currentType = buildUpdateWhereForClusteringColumn(clusteringColumnInfo, classSignature,
                    nextSignature);
            final List<TypeSpec> typeSpecs = buildWhereClassesForClusteringColumns(clusteringCols, classesSignature);
            typeSpecs.add(0, currentType);
            return typeSpecs;
        }
    }

    private static TypeSpec buildUpdateWhereForClusteringColumn(FieldSignatureInfo clusteringColumnInfo,
                                                                ClassSignatureInfo classSignature,
                                                                ClassSignatureInfo nextSignature) {

        final TypeSpec.Builder builder = TypeSpec.classBuilder(classSignature.className)
                .superclass(classSignature.superType)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(WHERE_CONSTRUCTOR)
                .addMethod(buildColumnRelation(EQ, nextSignature.returnClassType, clusteringColumnInfo));

        return builder.build();
    }


}

