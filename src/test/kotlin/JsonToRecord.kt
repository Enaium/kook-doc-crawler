/*
 * Copyright (c) 2024 Enaium
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import cn.enaium.jsonNode2Record
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.lang.model.element.Modifier

/**
 * @author Enaium
 */
class JsonToRecord {
    @Test
    fun test() {
        val json = """{
  "code": 0,
  "message": "操作成功",
  "data": {
    "items": [
      {
        "id": "7480000000000000",
        "user_id": "1700000",
        "parent_id": "",
        "name": "语音分组",
        "type": 0,
        "level": 100,
        "limit_amount": 0,
        "is_category": true
      },
      {
        "id": "3321010478582002",
        "user_id": "1700000",
        "parent_id": "7480000000000000",
        "name": "语音频道",
        "type": 2,
        "level": 100,
        "limit_amount": 25,
        "is_category": false
      }
    ],
    "meta": {
      "page": 1,
      "page_total": 1,
      "page_size": 50,
      "total": 2
    },
    "sort": []
  }
}""".trimIndent()

        val recordBuilder = TypeSpec.recordBuilder("Response")
            .addModifiers(Modifier.PUBLIC)
        jsonNode2Record(ObjectMapper().readTree(json), recordBuilder)

        Assertions.assertEquals(
            JavaFile.builder("cn.enaium", recordBuilder.build()).build().toString(), """package cn.enaium;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.util.List;

public record Response(@JsonProperty("code") Integer code, @JsonProperty("message") String message,
    @JsonProperty("data") Data data) {
  public record Data(@JsonProperty("items") List<Items> items, @JsonProperty("meta") Meta meta,
      @JsonProperty("sort") List<Object> sort) {
    public record Items(@JsonProperty("id") String id, @JsonProperty("user_id") String userId,
        @JsonProperty("parent_id") String parentId, @JsonProperty("name") String name,
        @JsonProperty("type") Integer type, @JsonProperty("level") Integer level,
        @JsonProperty("limit_amount") Integer limitAmount,
        @JsonProperty("is_category") Boolean isCategory) {
    }

    public record Meta(@JsonProperty("page") Integer page,
        @JsonProperty("page_total") Integer pageTotal, @JsonProperty("page_size") Integer pageSize,
        @JsonProperty("total") Integer total) {
    }
  }
}
"""
        )
    }
}