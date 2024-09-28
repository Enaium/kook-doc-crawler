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

package cn.enaium

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.squareup.javapoet.*
import org.jsoup.Jsoup
import java.net.URI
import java.nio.file.Paths
import java.util.*
import javax.lang.model.element.Modifier

const val SERVICE_PACKAGE = "cn.enaium.kookstarter.client.http"
const val RESPONSE_PACKAGE = "cn.enaium.kookstarter.model.response"
const val REQUEST_PACKAGE = "cn.enaium.kookstarter.model.request"

//const val OBJECTS_PACKAGE = "cn.enaium.kookstarter.model.objects"
const val EVENT_PACKAGE = "cn.enaium.kookstarter.model.event"

val packageToType = mutableListOf<Pair<String, TypeSpec.Builder>>()

fun main() {
    generateEvent("https://developer.kookapp.cn/doc/event/channel")
    generateEvent("https://developer.kookapp.cn/doc/event/direct-message")
    generateEvent("https://developer.kookapp.cn/doc/event/guild-member")
    generateEvent("https://developer.kookapp.cn/doc/event/guild-role")
    generateEvent("https://developer.kookapp.cn/doc/event/guild")
    generateEvent("https://developer.kookapp.cn/doc/event/message")
    generateEvent("https://developer.kookapp.cn/doc/event/user")
    generateService("https://developer.kookapp.cn/doc/http/guild")
    generateService("https://developer.kookapp.cn/doc/http/channel")
    generateService("https://developer.kookapp.cn/doc/http/message")
    generateService("https://developer.kookapp.cn/doc/http/channel-user")
    generateService("https://developer.kookapp.cn/doc/http/user-chat")
    generateService("https://developer.kookapp.cn/doc/http/direct-message")
    generateService("https://developer.kookapp.cn/doc/http/gateway")
    generateService("https://developer.kookapp.cn/doc/http/user")
    generateService("https://developer.kookapp.cn/doc/http/asset")
    generateService("https://developer.kookapp.cn/doc/http/guild-role")
    generateService("https://developer.kookapp.cn/doc/http/intimacy")
    generateService("https://developer.kookapp.cn/doc/http/guild-emoji")
    generateService("https://developer.kookapp.cn/doc/http/invite")
    generateService("https://developer.kookapp.cn/doc/http/blacklist")
    generateService("https://developer.kookapp.cn/doc/http/badge")
    generateService("https://developer.kookapp.cn/doc/http/game")
    generateService("https://developer.kookapp.cn/doc/http/oauth")

    val projectDir = System.getenv()["PROJECTS_DIR"]


    for ((packageName, typeBuilder) in packageToType) {
        JavaFile.builder(packageName, typeBuilder.build())
            .indent("    ")
            .build()
            .writeToPath(Paths.get(projectDir.toString(), "kook-spring-boot-starter", "src", "main", "java"))
    }
}

fun generateService(url: String) {
    val html = URI.create(url).toURL().readText()

    val doc = Jsoup.parse(html).getElementsByClass("developer-markdown").first()!!

    val title = doc.getElementsByTag("h1").first()!!.text()

    val interfaceBuilder =
        TypeSpec.interfaceBuilder(
            "${
                url.substring(url.lastIndexOf('/') + 1).snakeToCamelCase(firstCharUppercase = true)
            }Service"
        )
    interfaceBuilder.addModifiers(Modifier.PUBLIC)
    interfaceBuilder.addJavadoc("$title\n")
    for (element in doc.allElements) {
        if (element.nameIs("h2") && element.nextElementSibling()?.let { it.text() == "接口说明" } == true) {
            // API name
            val name = element.text()
            // API description
            element.nextElementSiblings().find { it.tagName() == "table" }?.let { descriptionTable ->
                val address =
                    descriptionTable.getElementsByTag("tbody").first()!!.getElementsByTag("tr").first()!!
                        .getElementsByTag("td")
                        .first()!!.text()
                val httpMethod = descriptionTable.getElementsByTag("tbody").first()!!.getElementsByTag("tr").first()!!
                    .getElementsByTag("td")[1].text()

                val shortPath = address.let {
                    if (it.startsWith("/api/v3/")) {
                        it.substring(8)
                    } else if (it.startsWith("/api/")) {
                        it.substring(5)
                    } else {
                        it
                    }
                }
                val returnType = "${shortPath.snakeToCamelCase(firstCharUppercase = true)}Response"
                val methodName = shortPath.snakeToCamelCase()

                // create a method builder
                val methodBuilder = MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addJavadoc("$name\n")
                    .addAnnotation(
                        AnnotationSpec.builder(
                            ClassName.get(
                                "org.springframework.web.service.annotation",
                                "${if (httpMethod == "GET") "Get" else "Post"}Exchange"
                            )
                        ).addMember("value", "\$S", address).build()
                    )
                    .returns(ClassName.get(RESPONSE_PACKAGE, returnType))

                // For return type
                val responseBuilder = TypeSpec.recordBuilder(returnType)
                    .addModifiers(Modifier.PUBLIC)

                // Add some common fields, such as code and message.
                // TMD, why the API use wrapper class for response

//                responseBuilder.addRecordComponent(
//                    ParameterSpec.builder(
//                        ClassName.get(java.lang.Integer::class.java),
//                        "code"
//                    ).addAnnotation(
//                        AnnotationSpec.builder(
//                            ClassName.get(
//                                "com.fasterxml.jackson.annotation",
//                                "JsonProperty"
//                            )
//                        ).addMember("value", "\$S", "code").build()
//                    ).build()
//                ).addRecordComponent(
//                    ParameterSpec.builder(
//                        ClassName.get(java.lang.String::class.java),
//                        "message"
//                    ).addAnnotation(
//                        AnnotationSpec.builder(
//                            ClassName.get(
//                                "com.fasterxml.jackson.annotation",
//                                "JsonProperty"
//                            )
//                        ).addMember("value", "\$S", "message").build()
//                    ).build()
//                )

                // Add response fields

                descriptionTable.nextElementSiblings()
                    .find { it.tagName() == "pre" && it.previousElementSibling()?.text() == "返回示例" }?.let { json ->
                        val dataText = json.text().let {
                            val replace = it.replace(Regex("[\n ]"), "")
                            //TMD, the response example is bad
                            if (replace.contains("{{") && replace.contains("}}")) {
                                replace.replace("{{", "{").replace("}}", "}")
                            } else if (replace.contains(",}")) {
                                replace.replace(",}", "}")
                            } else if (replace.contains(",]")) {
                                replace.replace(",]", "]")
                            } else {
                                it
                            }
                        }


                        val joinToString = dataText.lines()
                            .filter { !it.trimStart().startsWith("#") && !it.trimStart().startsWith("//") }
                            .joinToString("\n")
                        val jsonText = ObjectMapper().readTree(joinToString)

                        jsonNode2Record(jsonText, responseBuilder)

//                        if (jsonText.isCommonResponse.not()) return
//                        val data = jsonText.get("data")
//                        if (data.isObject) {
//                            val dataBuilder = TypeSpec.recordBuilder("Data")
//                                .addModifiers(Modifier.PUBLIC)
//                            if (data.isItem) {
//
//                                val items = data.get("items")
//
//                                if (items.isArray) {
//
//                                    val itemElement = items[0]
//
//                                    dataBuilder.addRecordComponent(
//                                        ParameterSpec.builder(
//                                            ParameterizedTypeName.get(
//                                                ClassName.get("java.util", "List"),
//                                                if (itemElement.isUser) {
//                                                    ClassName.get(OBJECTS_PACKAGE, "User")
//                                                } else if (itemElement.isRole) {
//                                                    ClassName.get(OBJECTS_PACKAGE, "Role")
//                                                } else if (itemElement.isChannel) {
//                                                    ClassName.get(OBJECTS_PACKAGE, "Channel")
//                                                } else if (itemElement.isGuild) {
//                                                    ClassName.get(OBJECTS_PACKAGE, "Guild")
//                                                } else {
//                                                    ClassName.get(java.lang.Object::class.java)
//                                                }
//                                            ),
//                                            "items"
//                                        ).addAnnotation(
//                                            AnnotationSpec.builder(
//                                                ClassName.get(
//                                                    "com.fasterxml.jackson.annotation",
//                                                    "JsonProperty"
//                                                )
//                                            ).addMember("value", "\$S", "items").build()
//                                        ).build()
//                                    )
//                                }
//
//
//                                dataBuilder.addRecordComponent(
//                                    ParameterSpec.builder(
//                                        ClassName.get(RESPONSE_PACKAGE, "Meta"),
//                                        "meta"
//                                    ).addAnnotation(
//                                        AnnotationSpec.builder(
//                                            ClassName.get(
//                                                "com.fasterxml.jackson.annotation",
//                                                "JsonProperty"
//                                            )
//                                        ).addMember("value", "\$S", "meta").build()
//                                    ).build()
//                                )
//
//                                dataBuilder.addRecordComponent(
//                                    ParameterSpec.builder(
//                                        ClassName.get(RESPONSE_PACKAGE, "Sort"),
//                                        "sort"
//                                    ).addAnnotation(
//                                        AnnotationSpec.builder(
//                                            ClassName.get(
//                                                "com.fasterxml.jackson.annotation",
//                                                "JsonProperty"
//                                            )
//                                        ).addMember("value", "\$S", "sort").build()
//                                    ).build()
//                                )
//                            }
//                            responseBuilder.addType(dataBuilder.build())
//                            responseBuilder.addRecordComponent(
//                                ParameterSpec.builder(
//                                    ClassName.get("", "Data"),
//                                    "data"
//                                ).addAnnotation(
//                                    AnnotationSpec.builder(
//                                        ClassName.get(
//                                            "com.fasterxml.jackson.annotation",
//                                            "JsonProperty"
//                                        )
//                                    ).addMember("value", "\$S", "data").build()
//                                ).build()
//                            )
//                        } else if (data.isArray) {
//                            responseBuilder.addRecordComponent(
//                                ParameterSpec.builder(
//                                    ParameterizedTypeName.get(
//                                        ClassName.get("java.util", "List"),
//                                        ClassName.get(java.lang.Object::class.java)
//                                    ),
//                                    "data"
//                                ).addAnnotation(
//                                    AnnotationSpec.builder(
//                                        ClassName.get(
//                                            "com.fasterxml.jackson.annotation",
//                                            "JsonProperty"
//                                        )
//                                    ).addMember("value", "\$S", "data").build()
//                                ).build()
//                            )
//                        } else {
//                            // Noting
//                        }
                    }

//                responseBuilder.addJavadoc("@param code    错误码，0代表成功，非0代表失败，具体的错误码参见错误码一览\n")
//                responseBuilder.addJavadoc("@param message 错误消息，具体的返回消息会根据Accept-Language来返回\n")
//                responseBuilder.addJavadoc("@param data    具体的数据\n")
                responseBuilder.addJavadoc("@author Enaium\n")
                responseBuilder.addJavadoc("@since 0.4.0\n")

                packageToType += RESPONSE_PACKAGE to responseBuilder


                // For method parameters
                descriptionTable.nextElementSiblings().find { it.tagName() == "table" }
                    ?.let { parameterTable ->
                        if (parameterTable.getElementsByTag("tbody").first()!!.text().isBlank()) {
                            return
                        }

                        // Add parameters to method
                        if (httpMethod == "GET") {
                            // API parameters
                            parameterTable.getElementsByTag("tbody").first()!!.getElementsByTag("tr")
                                .forEach { tr ->
                                    val parameterName = tr.getElementsByTag("td").first()!!.text()
                                    val parameterLocation = tr.getElementsByTag("td")[1].text()

                                    val typeIndex =
                                        parameterTable.getElementsByTag("thead").first()!!
                                            .getElementsByTag("tr").first()!!
                                            .getElementsByTag("th").indexOfFirst { it.text() == "类型" }

                                    val parameterType = tr.getElementsByTag("td")[typeIndex].text()
                                    val requireIndex =
                                        parameterTable.getElementsByTag("thead").first()!!
                                            .getElementsByTag("tr").first()!!
                                            .getElementsByTag("th")
                                            .indexOfFirst { it.text() == "必传" || it.text() == "必需" || it.text() == "是否必填" }// TMD
                                    val parameterRequired = tr.getElementsByTag("td")[requireIndex].text().toBool()
                                    val parameterDescription = tr.getElementsByTag("td").last()!!.text()

                                    methodBuilder.addJavadoc("@param ${parameterName.snakeToCamelCase()} $parameterDescription\n")
                                    val annotationBuilder = AnnotationSpec.builder(
                                        ClassName.get(
                                            "org.springframework.web.bind.annotation",
                                            "RequestParam"
                                        )
                                    )

                                    annotationBuilder.addMember("value", "\$S", parameterName)
                                    if (!parameterRequired) {
                                        annotationBuilder.addMember("required", "\$L", false)
                                    }

                                    // Add parameter to method
                                    methodBuilder.addParameter(
                                        ParameterSpec.builder(
                                            type(parameterType),
                                            parameterName.snakeToCamelCase()
                                        ).addAnnotation(
                                            ClassName.get(
                                                "org.jetbrains.annotations",
                                                if (parameterRequired) "NotNull" else "Nullable"
                                            )
                                        )
                                            .addAnnotation(
                                                annotationBuilder.build()
                                            )
                                            .build()
                                    )
                                }
                        } else if (httpMethod == "POST") {
                            val bodyType = "${shortPath.snakeToCamelCase(firstCharUppercase = true)}Body"

                            methodBuilder.addJavadoc("@param body 请求体\n")


                            // Create a record builder for request body
                            val requestBodyBuilder = TypeSpec.recordBuilder(bodyType)
                                .addModifiers(Modifier.PUBLIC)

                            parameterTable.getElementsByTag("tbody").first()!!.getElementsByTag("tr")
                                .forEach { tr ->

                                    val parameterName = tr.getElementsByTag("td").first()!!.text()
                                    val parameterLocation = tr.getElementsByTag("td")[1].text()

                                    val typeIndex =
                                        parameterTable.getElementsByTag("thead").first()!!
                                            .getElementsByTag("tr").first()!!
                                            .getElementsByTag("th").indexOfFirst { it.text() == "类型" }

                                    val parameterType = tr.getElementsByTag("td")[typeIndex].text()
                                    val requireIndex =
                                        parameterTable.getElementsByTag("thead").first()!!
                                            .getElementsByTag("tr").first()!!
                                            .getElementsByTag("th")
                                            .indexOfFirst { it.text() == "必传" || it.text() == "必需" || it.text() == "是否必填" }// TMD
                                    val parameterRequired = tr.getElementsByTag("td")[requireIndex].text().toBool()
                                    val parameterDescription = tr.getElementsByTag("td").last()!!.text()

                                    requestBodyBuilder.addJavadoc("@param ${parameterName.snakeToCamelCase()} $parameterDescription\n")
                                    requestBodyBuilder.addRecordComponent(
                                        ParameterSpec.builder(
                                            type(parameterType),
                                            parameterName.snakeToCamelCase()
                                        ).addAnnotation(
                                            AnnotationSpec.builder(
                                                ClassName.get(
                                                    "org.jetbrains.annotations",
                                                    if (parameterRequired) "NotNull" else "Nullable"
                                                )
                                            ).build()
                                        ).addAnnotation(
                                            AnnotationSpec.builder(
                                                ClassName.get(
                                                    "com.fasterxml.jackson.annotation",
                                                    "JsonProperty"
                                                )
                                            ).addMember("value", "\$S", parameterName).build()
                                        ).build()
                                    )
                                }

                            requestBodyBuilder.addJavadoc("@author Enaium\n")
                            requestBodyBuilder.addJavadoc("@since 0.4.0\n")
                            packageToType += REQUEST_PACKAGE to requestBodyBuilder

                            // Add parameter to method
                            methodBuilder.addParameter(
                                ParameterSpec.builder(
                                    ClassName.get(REQUEST_PACKAGE, bodyType),
                                    "body"
                                )
                                    .addAnnotation(
                                        ClassName.get(
                                            "org.jetbrains.annotations",
                                            "NotNull"
                                        )
                                    ).addAnnotation(
                                        ClassName.get(
                                            "org.springframework.web.bind.annotation",
                                            "RequestBody"
                                        )
                                    ).build()
                            )
                        }
                    }
                methodBuilder.addJavadoc("@since 0.4.0")
                interfaceBuilder.addMethod(methodBuilder.build())
            }
        }
    }
    interfaceBuilder.addJavadoc("@author Enaium\n")

    packageToType += SERVICE_PACKAGE to interfaceBuilder
}

fun generateEvent(url: String) {
    val html = URI.create(url).toURL().readText()

    val doc = Jsoup.parse(html).getElementsByClass("developer-markdown").first()!!

    val eventGroupName = url.substring(url.lastIndexOf('/') + 1).snakeToCamelCase(firstCharUppercase = true)
    val recordBuilder =
        TypeSpec.recordBuilder(
            "${
                eventGroupName
            }Event"
        ).addModifiers(Modifier.PUBLIC)

    for (allElement in doc.allElements) {
        if (allElement.nameIs("h2") && allElement.nextElementSiblings().find { it.tagName() == "pre" }
                ?.let {
                    it.previousElementSibling()!!.text() == "示例：" || it.previousElementSibling()!!.text()
                        .endsWith("消息示例")
                } == true) {
            val name = allElement.text()
            val json = allElement.nextElementSiblings().find { it.tagName() == "pre" }!!.text()
            val joinToString = json.lines()
                .filter { !it.trimStart().startsWith("#") && !it.trimStart().startsWith("//") }
                .joinToString("\n").replace("\n", "").replace(" ", "").replace(",}", "}")
            val jsonText = ObjectMapper().readTree(joinToString)
            val type = TypeSpec.recordBuilder(
                jsonText.get("d").get("extra").get("type")?.let {
                    when (it.nodeType) {
                        JsonNodeType.STRING -> {
                            it.asText().snakeToCamelCase(firstCharUppercase = true)
                        }

                        JsonNodeType.NUMBER -> {
                            "$eventGroupName$it"
                        }

                        else -> {
                            it.asText()
                        }
                    }
                }
            ).addModifiers(Modifier.PUBLIC)
            type.addJavadoc("$name\n")
            type.addJavadoc("@author Enaium\n")
            type.addJavadoc("@since 0.4.0\n")


            jsonNode2Record(jsonText, type)
            recordBuilder.addType(type.build())
        }
    }
    packageToType += EVENT_PACKAGE to recordBuilder
}

fun type(type: String): ClassName {
    return when (type.lowercase()) {
        "string" -> ClassName.get(java.lang.String::class.java)
        "int", "integer" -> ClassName.get(java.lang.Integer::class.java)
        "long" -> ClassName.get(java.lang.Long::class.java)
        "float" -> ClassName.get(java.lang.Float::class.java)
        "double" -> ClassName.get(java.lang.Double::class.java)
        "boolean", "bool" -> ClassName.get(java.lang.Boolean::class.java)
        else -> ClassName.get(java.lang.Object::class.java)
    }
}

fun String.snakeToCamelCase(
    firstCharUppercase: Boolean = false,
): String {
    return this.split(Regex("[/_-]"))
        .joinToString("") {
            it.replaceFirstChar { firstChar -> firstChar.uppercase(Locale.getDefault()) }
        }.let {
            if (!firstCharUppercase) {
                it.replaceFirstChar { firstChar -> firstChar.lowercase(Locale.getDefault()) }
            } else {
                it
            }
        }
}

fun String.toBool(): Boolean {
    return when (this.lowercase()) {
        "true", "是" -> true
        "false", "否" -> false
        else -> false
    }
}

val JsonNode.isUser: Boolean
    get() = (this.has("id")
            && this.has("username")
            && this.has("nickname")
            && this.has("identify_num"))
val JsonNode.isRole: Boolean
    get() = (this.has("role_id")
            && this.has("name")
            && this.has("color")
            && this.has("position")
            && this.has("permissions"))
val JsonNode.isChannel: Boolean
    get() = (this.has("id")
            && this.has("name")
            && this.has("user_id")
            && this.has("guild_id")
            && this.has("topic")
            && this.has("is_category")
            && this.has("parent_id")
            && this.has("level"))
val JsonNode.isGuild: Boolean
    get() = (this.has("id")
            && this.has("name")
            && this.has("topic")
            && this.has("user_id")
            && this.has("icon")
            && this.has("default_channel_id"))
val JsonNode.isCommonResponse: Boolean
    get() = (this.has("code")
            && this.has("message")
            && this.has("data"))
val JsonNode.isItem: Boolean
    get() = (this.has("items")
            && this.has("meta")
            && this.has("sort"))


fun jsonNode2Record(jsonNode: JsonNode, typeSpecBuilder: TypeSpec.Builder) {
    jsonNode.fields().forEach { (key, value) ->
        if (value.nodeType == JsonNodeType.OBJECT) {
            val recordBuilder = TypeSpec.recordBuilder(key.snakeToCamelCase(firstCharUppercase = true))
                .addModifiers(Modifier.PUBLIC)
            jsonNode2Record(value, recordBuilder)
            typeSpecBuilder.addRecordComponent(
                ParameterSpec.builder(
                    ClassName.get("", key.snakeToCamelCase(firstCharUppercase = true)),
                    key.snakeToCamelCase()
                ).addAnnotation(
                    AnnotationSpec.builder(
                        ClassName.get(
                            "com.fasterxml.jackson.annotation",
                            "JsonProperty"
                        )
                    ).addMember("value", "\$S", key).build()
                ).build()
            )
            typeSpecBuilder.addType(recordBuilder.build())
        } else if (value.nodeType == JsonNodeType.ARRAY) {
            val recordBuilder = TypeSpec.recordBuilder(key.snakeToCamelCase(firstCharUppercase = true))
                .addModifiers(Modifier.PUBLIC)
            val element = value[0] ?: ObjectMapper().readTree("[]")

            if (element.nodeType == JsonNodeType.OBJECT) {
                jsonNode2Record(element, recordBuilder)
                typeSpecBuilder.addRecordComponent(
                    ParameterSpec.builder(
                        ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"),
                            ClassName.get("", key.snakeToCamelCase(firstCharUppercase = true))
                        ),
                        key.snakeToCamelCase()
                    ).addAnnotation(
                        AnnotationSpec.builder(
                            ClassName.get(
                                "com.fasterxml.jackson.annotation",
                                "JsonProperty"
                            )
                        ).addMember("value", "\$S", key).build()
                    ).build()
                )
                typeSpecBuilder.addType(recordBuilder.build())
            } else {
                typeSpecBuilder.addRecordComponent(
                    ParameterSpec.builder(
                        ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"),
                            when (element.nodeType) {
                                JsonNodeType.BOOLEAN -> ClassName.get(java.lang.Boolean::class.java)
                                JsonNodeType.NUMBER -> {
                                    if (element.canConvertToInt()) {
                                        ClassName.get(java.lang.Integer::class.java)
                                    } else {
                                        ClassName.get(java.lang.Long::class.java)
                                    }
                                }

                                JsonNodeType.STRING -> ClassName.get(java.lang.String::class.java)
                                else -> ClassName.get(java.lang.Object::class.java)
                            }
                        ),
                        key.snakeToCamelCase()
                    ).addAnnotation(
                        AnnotationSpec.builder(
                            ClassName.get(
                                "com.fasterxml.jackson.annotation",
                                "JsonProperty"
                            )
                        ).addMember("value", "\$S", key).build()
                    ).build()
                )
            }
        } else {
            typeSpecBuilder.addRecordComponent(
                ParameterSpec.builder(
                    when (value.nodeType) {
                        JsonNodeType.BOOLEAN -> ClassName.get(java.lang.Boolean::class.java)
                        JsonNodeType.NUMBER -> {
                            if (value.canConvertToInt()) {
                                ClassName.get(java.lang.Integer::class.java)
                            } else {
                                ClassName.get(java.lang.Long::class.java)
                            }
                        }

                        JsonNodeType.STRING -> ClassName.get(java.lang.String::class.java)
                        else -> ClassName.get(java.lang.Object::class.java)
                    },
                    key.snakeToCamelCase()
                ).addAnnotation(
                    AnnotationSpec.builder(
                        ClassName.get(
                            "com.fasterxml.jackson.annotation",
                            "JsonProperty"
                        )
                    ).addMember("value", "\$S", key).build()
                ).build()
            )
        }
    }
}