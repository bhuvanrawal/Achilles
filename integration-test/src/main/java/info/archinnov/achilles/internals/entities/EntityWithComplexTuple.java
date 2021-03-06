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

package info.archinnov.achilles.internals.entities;

import java.util.List;
import java.util.Map;

import info.archinnov.achilles.annotations.Column;
import info.archinnov.achilles.annotations.Table;
import info.archinnov.achilles.annotations.PartitionKey;
import info.archinnov.achilles.type.tuples.Tuple2;

@Table(table = "complex_tuple")
public class EntityWithComplexTuple {

    @PartitionKey
    private Long id;

    @Column
    private Tuple2<Integer, Map<Integer, List<String>>> tuple;

    public EntityWithComplexTuple() {
    }

    public EntityWithComplexTuple(Long id, Tuple2<Integer, Map<Integer, List<String>>> tuple) {
        this.id = id;
        this.tuple = tuple;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tuple2<Integer, Map<Integer, List<String>>> getTuple() {
        return tuple;
    }

    public void setTuple(Tuple2<Integer, Map<Integer, List<String>>> tuple) {
        this.tuple = tuple;
    }
}
