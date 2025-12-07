/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.parquet;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.types.Types.DoubleType;
import org.apache.iceberg.types.Types.IntegerType;
import org.apache.iceberg.types.Types.ListType;
import org.apache.iceberg.types.Types.MapType;
import org.apache.iceberg.types.Types.NestedField;
import org.apache.iceberg.types.Types.StringType;
import org.apache.iceberg.types.Types.StructType;
import org.apache.iceberg.types.Types.VariantType;
import org.apache.iceberg.variants.Variant;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.junit.jupiter.api.Test;

public class TestPruneColumns {
  @Test
  public void testMapKeyValueName() {
    // Test case: map with struct value, project only value.x and value.y (not z)
    //
    // With getProjectedIdsExcludingStructs(projection), selectedIds = {2, 4, 5}
    // (primitive field IDs: key, x, y)
    // Container IDs (1 for map, 3 for value struct) are NOT included.
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.primitive(PrimitiveTypeName.BINARY, Type.Repetition.REQUIRED)
                                    .as(LogicalTypeAnnotation.stringType())
                                    .id(2)
                                    .named("key"))
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(4)
                                            .named("x"))
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(5)
                                            .named("y"))
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(6)
                                            .named("z"))
                                    .id(3)
                                    .named("value"))
                            .named("custom_key_value_name"))
                    .as(LogicalTypeAnnotation.mapType())
                    .id(1)
                    .named("m"))
            .named("table");

    // project map.value.x and map.value.y
    Schema projection =
        new Schema(
            NestedField.optional(
                1,
                "m",
                MapType.ofOptional(
                    2,
                    3,
                    StringType.get(),
                    StructType.of(
                        NestedField.required(4, "x", DoubleType.get()),
                        NestedField.required(5, "y", DoubleType.get())))));

    MessageType expected =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.primitive(PrimitiveTypeName.BINARY, Type.Repetition.REQUIRED)
                                    .as(LogicalTypeAnnotation.stringType())
                                    .id(2)
                                    .named("key"))
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(4)
                                            .named("x"))
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(5)
                                            .named("y"))
                                    .id(3)
                                    .named("value"))
                            .named("custom_key_value_name"))
                    .as(LogicalTypeAnnotation.mapType())
                    .id(1)
                    .named("m"))
            .named("table");

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual).as("Pruned schema should not rename repeated struct").isEqualTo(expected);
  }

  @Test
  public void testListElementName() {
    // Test case: list of structs, project only x and y (not z)
    //
    // With getProjectedIdsExcludingStructs(projection), selectedIds = {4, 5}
    // (only primitive field IDs: x, y)
    // Container IDs (1, 3) are NOT included.
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(4)
                                            .named("x"))
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(5)
                                            .named("y"))
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(6)
                                            .named("z"))
                                    .id(3)
                                    .named("custom_element_name"))
                            .named("custom_repeated_name"))
                    .as(LogicalTypeAnnotation.listType())
                    .id(1)
                    .named("m"))
            .named("table");

    // project map.value.x and map.value.y
    Schema projection =
        new Schema(
            NestedField.optional(
                1,
                "m",
                ListType.ofOptional(
                    3,
                    StructType.of(
                        NestedField.required(4, "x", DoubleType.get()),
                        NestedField.required(5, "y", DoubleType.get())))));

    MessageType expected =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(4)
                                            .named("x"))
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                                            .id(5)
                                            .named("y"))
                                    .id(3)
                                    .named("custom_element_name"))
                            .named("custom_repeated_name"))
                    .as(LogicalTypeAnnotation.listType())
                    .id(1)
                    .named("m"))
            .named("table");

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual).as("Pruned schema should not rename repeated struct").isEqualTo(expected);
  }

  @Test
  public void testNestedListWithStructPruning() {
    // Test case: list of structs where each struct contains another list of structs
    // Similar to: pv_requests: list<struct<available, servedItems: list<struct<boosts, clicked,
    // ...other fields>>>>
    //
    // The projection Schema must contain the full nested structure (you can't have `boosts`
    // without its parent `servedItems` list). But getProjectedIdsExcludingStructs extracts
    // only the leaf/primitive IDs from that structure:
    //
    // With getProjectedIds(projection) [includeStructIds=true]:
    //   Returns: {1, 3, 4, 5, 7, 8, 9} - all field IDs including containers
    //
    // With getProjectedIdsExcludingStructs(projection) [includeStructIds=false]:
    //   Returns: {4, 8, 9} - only primitive field IDs (available, boosts, clicked)
    //   Container IDs (1=pv_requests, 3=element, 5=servedItems, 7=inner element) are NOT included.
    //
    // Expected behavior: nested pruning should work - other_field_1 and other_field_2
    // should be removed even though their parent struct (ID=7) is not in selectedIds.
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.BOOLEAN, Type.Repetition.OPTIONAL)
                                            .id(4)
                                            .named("available"))
                                    .addField(
                                        Types.buildGroup(Type.Repetition.OPTIONAL)
                                            .addField(
                                                Types.buildGroup(Type.Repetition.REPEATED)
                                                    .addField(
                                                        Types.buildGroup(Type.Repetition.OPTIONAL)
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(8)
                                                                    .named("boosts"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.BOOLEAN,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(9)
                                                                    .named("clicked"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(10)
                                                                    .named("other_field_1"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(11)
                                                                    .named("other_field_2"))
                                                            .id(7)
                                                            .named("element"))
                                                    .named("list"))
                                            .as(LogicalTypeAnnotation.listType())
                                            .id(5)
                                            .named("servedItems"))
                                    .id(3)
                                    .named("element"))
                            .named("list"))
                    .as(LogicalTypeAnnotation.listType())
                    .id(1)
                    .named("pv_requests"))
            .named("table");

    // Project only: pv_requests[].available and pv_requests[].servedItems[].boosts, clicked
    Schema projection =
        new Schema(
            NestedField.optional(
                1,
                "pv_requests",
                ListType.ofOptional(
                    3,
                    StructType.of(
                        NestedField.optional(
                            4, "available", org.apache.iceberg.types.Types.BooleanType.get()),
                        NestedField.optional(
                            5,
                            "servedItems",
                            ListType.ofOptional(
                                7,
                                StructType.of(
                                    NestedField.optional(8, "boosts", DoubleType.get()),
                                    NestedField.optional(
                                        9,
                                        "clicked",
                                        org.apache.iceberg.types.Types.BooleanType.get()))))))));

    // Expected: only available, servedItems with boosts and clicked (no other_field_1,
    // other_field_2)
    MessageType expected =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.BOOLEAN, Type.Repetition.OPTIONAL)
                                            .id(4)
                                            .named("available"))
                                    .addField(
                                        Types.buildGroup(Type.Repetition.OPTIONAL)
                                            .addField(
                                                Types.buildGroup(Type.Repetition.REPEATED)
                                                    .addField(
                                                        Types.buildGroup(Type.Repetition.OPTIONAL)
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(8)
                                                                    .named("boosts"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.BOOLEAN,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(9)
                                                                    .named("clicked"))
                                                            .id(7)
                                                            .named("element"))
                                                    .named("list"))
                                            .as(LogicalTypeAnnotation.listType())
                                            .id(5)
                                            .named("servedItems"))
                                    .id(3)
                                    .named("element"))
                            .named("list"))
                    .as(LogicalTypeAnnotation.listType())
                    .id(1)
                    .named("pv_requests"))
            .named("table");

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual)
        .as("Pruned schema should remove other_field_1 and other_field_2 from nested list")
        .isEqualTo(expected);
  }

  @Test
  public void testExplicitStructSelectionPreservesAllFields() {
    // Test case: SELECT point FROM ... (projecting entire struct with all fields)
    //
    // With getProjectedIdsExcludingStructs(projection), selectedIds = {3, 4, 5}
    // (only primitive field IDs: x, y, z)
    // Container ID (2 for point struct) is NOT included.
    //
    // Since all leaf fields of the struct are in the projection, the full struct is preserved.
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.primitive(PrimitiveTypeName.INT32, Type.Repetition.REQUIRED)
                    .id(1)
                    .named("id"))
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(3)
                            .named("x"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(4)
                            .named("y"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(5)
                            .named("z"))
                    .id(2)
                    .named("point"))
            .named("table");

    // Project the whole struct (all fields) - simulates SELECT point, point.x FROM ...
    // The struct is explicitly selected with all its fields
    Schema projection =
        new Schema(
            NestedField.optional(
                2,
                "point",
                StructType.of(
                    NestedField.required(3, "x", DoubleType.get()),
                    NestedField.required(4, "y", DoubleType.get()),
                    NestedField.required(5, "z", DoubleType.get()))));

    // Expected: full struct with all fields preserved
    MessageType expected =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(3)
                            .named("x"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(4)
                            .named("y"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(5)
                            .named("z"))
                    .id(2)
                    .named("point"))
            .named("table");

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual)
        .as("Full struct should be preserved when explicitly selected")
        .isEqualTo(expected);
  }

  @Test
  public void testNestedListExplicitStructSelectionPreservesAllFields() {
    // Test case: Using the same nested schema as testNestedListWithStructPruning,
    // but selecting the full inner struct element with ALL its fields.
    // This simulates: SELECT pv_requests[].servedItems[] FROM ...
    //
    // With getProjectedIdsExcludingStructs(projection), selectedIds = {4, 8, 9, 10, 11}
    // (all primitive field IDs: available, boosts, clicked, other_field_1, other_field_2)
    // Container IDs (1, 3, 5, 7) are NOT included.
    //
    // Since all leaf fields are in the projection, the full schema is preserved.
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.buildGroup(Type.Repetition.REPEATED)
                            .addField(
                                Types.buildGroup(Type.Repetition.OPTIONAL)
                                    .addField(
                                        Types.primitive(
                                                PrimitiveTypeName.BOOLEAN, Type.Repetition.OPTIONAL)
                                            .id(4)
                                            .named("available"))
                                    .addField(
                                        Types.buildGroup(Type.Repetition.OPTIONAL)
                                            .addField(
                                                Types.buildGroup(Type.Repetition.REPEATED)
                                                    .addField(
                                                        Types.buildGroup(Type.Repetition.OPTIONAL)
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(8)
                                                                    .named("boosts"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.BOOLEAN,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(9)
                                                                    .named("clicked"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(10)
                                                                    .named("other_field_1"))
                                                            .addField(
                                                                Types.primitive(
                                                                        PrimitiveTypeName.DOUBLE,
                                                                        Type.Repetition.OPTIONAL)
                                                                    .id(11)
                                                                    .named("other_field_2"))
                                                            .id(7)
                                                            .named("element"))
                                                    .named("list"))
                                            .as(LogicalTypeAnnotation.listType())
                                            .id(5)
                                            .named("servedItems"))
                                    .id(3)
                                    .named("element"))
                            .named("list"))
                    .as(LogicalTypeAnnotation.listType())
                    .id(1)
                    .named("pv_requests"))
            .named("table");

    // Project ALL fields of the inner struct - simulates selecting the full struct
    Schema projection =
        new Schema(
            NestedField.optional(
                1,
                "pv_requests",
                ListType.ofOptional(
                    3,
                    StructType.of(
                        NestedField.optional(
                            4, "available", org.apache.iceberg.types.Types.BooleanType.get()),
                        NestedField.optional(
                            5,
                            "servedItems",
                            ListType.ofOptional(
                                7,
                                StructType.of(
                                    NestedField.optional(8, "boosts", DoubleType.get()),
                                    NestedField.optional(
                                        9,
                                        "clicked",
                                        org.apache.iceberg.types.Types.BooleanType.get()),
                                    NestedField.optional(10, "other_field_1", DoubleType.get()),
                                    NestedField.optional(
                                        11, "other_field_2", DoubleType.get()))))))));

    // Expected: ALL fields preserved since the full struct was explicitly selected
    MessageType expected = fileSchema;

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual)
        .as("All fields should be preserved when full nested struct is explicitly selected")
        .isEqualTo(expected);
  }

  @Test
  public void testStructElementName() {
    // Test case: project only struct_name_1.y and struct_name_1.z (not x, not struct_name_2)
    //
    // With getProjectedIdsExcludingStructs(projection), selectedIds = {4, 5}
    // (only primitive field IDs: y, z)
    // Container ID (2 for struct_name_1) is NOT included.
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                    .id(1)
                    .named("id"))
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(3)
                            .named("x"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(4)
                            .named("y"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(5)
                            .named("z"))
                    .id(2)
                    .named("struct_name_1"))
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(7)
                            .named("x"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(8)
                            .named("y"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(9)
                            .named("z"))
                    .id(6)
                    .named("struct_name_2"))
            .named("table");

    // project only struct_name_1.y and struct_name_1.z (not struct_name_2)
    Schema projection =
        new Schema(
            NestedField.optional(
                2,
                "struct_name_1",
                StructType.of(
                    NestedField.required(4, "y", DoubleType.get()),
                    NestedField.required(5, "z", DoubleType.get()))));

    MessageType expected =
        Types.buildMessage()
            .addField(
                Types.buildGroup(Type.Repetition.OPTIONAL)
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(4)
                            .named("y"))
                    .addField(
                        Types.primitive(PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED)
                            .id(5)
                            .named("z"))
                    .id(2)
                    .named("struct_name_1"))
            .named("table");

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual).as("Pruned schema should be matched").isEqualTo(expected);
  }

  @Test
  public void testVariant() {
    MessageType fileSchema =
        Types.buildMessage()
            .addField(
                Types.primitive(PrimitiveTypeName.INT32, Type.Repetition.REQUIRED)
                    .id(1)
                    .named("id"))
            .addField(buildVariantType(2, "variant_1"))
            .addField(buildVariantType(3, "variant_2"))
            .named("table");

    Schema projection =
        new Schema(
            ImmutableList.of(
                NestedField.required(1, "id", IntegerType.get()),
                NestedField.required(2, "variant_1", VariantType.get())));
    MessageType expected =
        Types.buildMessage()
            .addField(
                Types.primitive(PrimitiveTypeName.INT32, Type.Repetition.REQUIRED)
                    .id(1)
                    .named("id"))
            .addField(buildVariantType(2, "variant_1"))
            .named("table");

    MessageType actual = ParquetSchemaUtil.pruneColumns(fileSchema, projection);
    assertThat(actual).as("Pruned schema should be matched").isEqualTo(expected);
  }

  private static Type buildVariantType(int id, String name) {
    return Types.buildGroup(Type.Repetition.OPTIONAL)
        .as(LogicalTypeAnnotation.variantType(Variant.VARIANT_SPEC_VERSION))
        .addField(
            Types.primitive(PrimitiveTypeName.BINARY, Type.Repetition.REQUIRED).named("metadata"))
        .addField(
            Types.primitive(PrimitiveTypeName.BINARY, Type.Repetition.REQUIRED).named("value"))
        .id(id)
        .named(name);
  }
}
