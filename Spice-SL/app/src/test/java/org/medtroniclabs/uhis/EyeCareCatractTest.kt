package org.medtroniclabs.uhis

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Validates dynamic form-layout JSON files (eye care, cataract, NCD, etc.)
 *
 * Catches the two classes of bug that silently break conditional visibility
 * and downstream FHIR/SQL indicator logic:
 *
 *   1. REFERENCE INTEGRITY  - every value used in a field's own `condition`
 *      (`eq` / `eqList`) must match a real option `id` declared in that same
 *      field's `optionsList`. A mismatch means the condition can never fire.
 *
 *   2. CASING / NAMING       - every option `id` must follow lowerCamelCase
 *      (or a recognised acronym), so the data layer can match values reliably.
 *
 * It also checks `multipleParents` references resolve to real option ids on
 * the named parent field, with matching case.
 *
 * This is a pure JVM unit test (src/test) - it reads the same JSON the app
 * ships in src/main/assets, so there is a single source of truth and no
 * duplicated fixtures. No emulator / Robolectric required.
 */
class EyeCareCatractTest {
    private val gson = Gson()

    // ---- configuration ------------------------------------------------------

    /** Forms to validate. Add new form filenames here as they are created. */
    private val formFiles = listOf(
        "eye_care.json",
        "cataract.json",
    )

    /**
     * Field ids that are intentionally exempt from the lowerCamelCase rule
     * because existing code already depends on their current value.
     */
    private val fieldIdExemptions = setOf(
        "camp_type",
        "camp_date",
    )

    /**
     * Option ids that are intentionally exempt from the lowerCamelCase rule.
     * - camp_type options: existing code built on "Combined" / "Single"
     * - glass power options are numeric-with-symbol values, not names
     * - SK / SS are recognised acronyms
     * - yes / no and boolean true/false are valid tokens
     */
    private val optionIdExemptions = setOf(
        "Combined",
        "Single", // camp_type
        "SK",
        "SS", // whoReferredThisPerson acronyms
        "fbs",
        "rbs", // investigation acronyms
        "dct",
        "dcr", // operation acronyms
        "highIOP", // contains acronym IOP - acceptable
    )

    // lowerCamelCase: starts lowercase, letters/digits only, no separators
    private val camelCaseRegex = Regex("^[a-z][a-zA-Z0-9]*$")

    // glass-power style numeric option (e.g. +1.00) - allowed
    private val numericOptionRegex = Regex("^[+-]?[0-9]+(\\.[0-9]+)?$")

    // ---- locating the asset files ------------------------------------------

    private fun assetsDir(): File {
        // Common locations relative to the module's working dir when tests run.
        val candidates = listOf(
            "src/main/assets",
            "src/main/assets/forms",
            "app/src/main/assets",
            "app/src/main/assets/forms",
            "src/test/resources/forms",
        )
        return candidates.map { File(it) }.firstOrNull { it.isDirectory }
            ?: fail("Could not locate assets dir. Tried: $candidates").let { error("unreachable") }
    }

    private fun loadForm(fileName: String): JsonObject {
        val dir = assetsDir()
        val file = File(dir, fileName)
        if (!file.exists()) {
            // also try a recursive search in case it's nested
            val found = dir.walkTopDown().firstOrNull { it.name == fileName }
                ?: fail("Form file not found: $fileName under ${dir.path}").let { error("unreachable") }
            return gson.fromJson(found.readText(), JsonObject::class.java)
        }
        return gson.fromJson(file.readText(), JsonObject::class.java)
    }

    // ---- helpers ------------------------------------------------------------

    private data class Field(
        val id: String,
        val obj: JsonObject,
        val optionIds: List<String>,
    )

    private fun parseFields(form: JsonObject): List<Field> {
        val layout = form.getAsJsonArray("formLayout")
            ?: fail("formLayout array missing").let { error("unreachable") }
        return layout.mapNotNull { el ->
            val o = el.asJsonObject
            val id = o.get("id")?.asString ?: return@mapNotNull null
            val optionIds = o.getAsJsonArray("optionsList")?.map { opt ->
                val v = opt.asJsonObject.get("id")
                // id can be string OR boolean (e.g. isRegularSmoker)
                if (v.isJsonPrimitive && v.asJsonPrimitive.isBoolean) {
                    v.asBoolean.toString()
                } else {
                    v.asString
                }
            } ?: emptyList()
            Field(id, o, optionIds)
        }
    }

    private fun conditionValues(condition: JsonArray?): List<String> {
        if (condition == null) return emptyList()
        val values = mutableListOf<String>()
        condition.forEach { c ->
            val co = c.asJsonObject
            co.get("eq")?.let { values.add(it.asString) }
            co.getAsJsonArray("eqList")?.forEach { values.add(it.asString) }
        }
        return values
    }

    private fun isBooleanOptionField(field: Field): Boolean {
        // optionType == "boolean" fields use true/false ids - skip casing check
        return field.obj.get("optionType")?.asString == "boolean"
    }

    // ---- the tests ----------------------------------------------------------

    @Test
    fun `every option id follows lowerCamelCase or is exempt`() {
        val errors = mutableListOf<String>()

        formFiles.forEach { fileName ->
            val fields = parseFields(loadForm(fileName))
            fields.forEach { field ->
                if (isBooleanOptionField(field)) return@forEach // true/false ids
                if (field.id in fieldIdExemptions) return@forEach

                field.optionIds.forEach { optId ->
                    val ok = optId in optionIdExemptions ||
                        camelCaseRegex.matches(optId) ||
                        numericOptionRegex.matches(optId)
                    if (!ok) {
                        errors.add("[$fileName] field '${field.id}' has non-camelCase option id: '$optId'")
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            fail("Option id naming violations:\n" + errors.joinToString("\n"))
        }
    }

    @Test
    fun `every condition eq and eqList value matches an option id in the same field`() {
        val errors = mutableListOf<String>()

        formFiles.forEach { fileName ->
            val fields = parseFields(loadForm(fileName))
            fields.forEach { field ->
                val condition = field.obj.getAsJsonArray("condition")
                val referenced = conditionValues(condition)
                val ownIds = field.optionIds.toSet()

                referenced.forEach { value ->
                    if (value !in ownIds) {
                        errors.add(
                            "[$fileName] field '${field.id}' condition references '$value' " +
                                "which is not an option id of that field. Options: $ownIds",
                        )
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            fail("Condition / eqList reference mismatches:\n" + errors.joinToString("\n"))
        }
    }

    @Test
    fun `every multipleParents reference resolves to a real option id on the named parent`() {
        val errors = mutableListOf<String>()

        formFiles.forEach { fileName ->
            val fields = parseFields(loadForm(fileName))
            val byId = fields.associateBy { it.id }

            fields.forEach { field ->
                val mp = field.obj.getAsJsonObject("multipleParents") ?: return@forEach
                mp.entrySet().forEach { (parentId, valuesEl) ->
                    val parent = byId[parentId]
                    if (parent == null) {
                        errors.add("[$fileName] field '${field.id}' multipleParents names unknown parent '$parentId'")
                        return@forEach
                    }
                    val parentIds = parent.optionIds.toSet()
                    valuesEl.asJsonArray.forEach { v ->
                        val value = v.asString
                        if (value !in parentIds) {
                            errors.add(
                                "[$fileName] field '${field.id}' multipleParents['$parentId'] references " +
                                    "'$value' not found in parent options $parentIds",
                            )
                        }
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            fail("multipleParents reference mismatches:\n" + errors.joinToString("\n"))
        }
    }

    @Test
    fun `every targetId in a condition points to a field that exists`() {
        val errors = mutableListOf<String>()

        formFiles.forEach { fileName ->
            val fields = parseFields(loadForm(fileName))
            val allIds = fields.map { it.id }.toSet()

            fields.forEach { field ->
                field.obj.getAsJsonArray("condition")?.forEach { c ->
                    val target = c.asJsonObject.get("targetId")?.asString
                    if (target != null && target !in allIds) {
                        errors.add("[$fileName] field '${field.id}' condition targetId '$target' does not exist")
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            fail("Dangling targetId references:\n" + errors.joinToString("\n"))
        }
    }
}
