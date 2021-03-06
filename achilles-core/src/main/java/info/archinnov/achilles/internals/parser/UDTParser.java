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

package info.archinnov.achilles.internals.parser;

import static info.archinnov.achilles.internals.parser.TypeUtils.getRawType;
import static info.archinnov.achilles.internals.parser.TypeUtils.*;
import static info.archinnov.achilles.internals.parser.validator.BeanValidator.validateHasPublicConstructor;
import static info.archinnov.achilles.internals.parser.validator.BeanValidator.validateIsAConcreteNonFinalClass;

import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import info.archinnov.achilles.annotations.UDT;
import info.archinnov.achilles.internals.apt.AptUtils;
import info.archinnov.achilles.internals.codegen.meta.UDTMetaCodeGen;
import info.archinnov.achilles.internals.parser.FieldParser.TypeParsingResult;
import info.archinnov.achilles.internals.parser.context.EntityParsingContext;
import info.archinnov.achilles.internals.parser.context.FieldParsingContext;

public class UDTParser extends AbstractBeanParser {

    private final UDTMetaCodeGen udtMetaCodeGen;

    public UDTParser(AptUtils aptUtils) {
        super(aptUtils);
        this.udtMetaCodeGen = new UDTMetaCodeGen(aptUtils);
    }

    public TypeParsingResult parseUDT(AnnotationTree annotationTree, FieldParsingContext context, FieldParser fieldParser) {
        final TypeMirror typeMirror = annotationTree.getCurrentType();
        final TypeName udtTypeName = TypeName.get(typeMirror);
        final TypeName rawUdtTypeName = getRawType(udtTypeName);
        final TypeElement typeElement = aptUtils.asTypeElement(typeMirror);

        validateUDT(rawUdtTypeName, typeElement);

        if (!context.hasProcessedUDT(rawUdtTypeName)) {
            final TypeSpec udtClassPropertyCode = buildUDTClassProperty(typeElement, fieldParser, context.entityContext);
            context.addUDTMeta(rawUdtTypeName, udtClassPropertyCode);
        }

        CodeBlock typeCode = CodeBlock.builder().add("new $T<$T, $T>($L, $T.class, $L.INSTANCE)",
                UDT_PROPERTY,
                context.entityRawType,
                rawUdtTypeName.box(),
                context.fieldInfoCode,
                rawUdtTypeName.box(),
                UDT_META_PACKAGE + "." + typeElement.getSimpleName() + META_SUFFIX)
                .build();
        final ParameterizedTypeName propertyType = genericType(UDT_PROPERTY, context.entityRawType, rawUdtTypeName);
        return new TypeParsingResult(context, annotationTree.hasNext() ? annotationTree.next() : annotationTree,
                udtTypeName, JAVA_DRIVER_UDT_VALUE_TYPE, propertyType, typeCode);
    }

    void validateUDT(TypeName udtTypeName, TypeElement typeElement) {
        validateIsAConcreteNonFinalClass(aptUtils, typeElement);
        final boolean isSupportedType = TypeUtils.ALLOWED_TYPES.contains(udtTypeName);
        aptUtils.validateFalse(isSupportedType,
                "Type '%s' cannot be annotated with '%s' because it is a supported type",
                udtTypeName, UDT.class.getCanonicalName());
        validateHasPublicConstructor(aptUtils, udtTypeName, typeElement);
    }

    TypeSpec buildUDTClassProperty(TypeElement elm, FieldParser fieldParser, EntityParsingContext context) {
        final List<TypeParsingResult> parsingResults = parseFields(elm, fieldParser, context.globalContext);
        return udtMetaCodeGen.buildUDTClassProperty(elm, context, parsingResults);
    }
}
