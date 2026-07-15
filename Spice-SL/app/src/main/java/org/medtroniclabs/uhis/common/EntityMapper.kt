package org.medtroniclabs.uhis.common

import org.medtroniclabs.uhis.data.CountryModel
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.ProgramEntity
import org.medtroniclabs.uhis.db.entity.ChiefDomEntity
import org.medtroniclabs.uhis.db.entity.DistrictEntity
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaKormiEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaEntity
import org.medtroniclabs.uhis.db.entity.SignsAndSymptomsEntity
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity

object EntityMapper {
    fun getResultSpinnerMapList(data: LocalSpinnerResponse): ArrayList<Map<String, Any>> {
        if (data.response is List<*>) {
            return ArrayList(
                data.response.map { properties ->
                    val map = HashMap<String, Any>()
                    mapsIdName(properties, map)
                    map
                },
            )
        }
        return ArrayList()
    }

    private fun mapsIdName(
        properties: Any?,
        map: HashMap<String, Any>,
    ) {
        when (properties) {
            is SiteEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is ChiefDomEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is VillageEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is SubVillageEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is HealthFacilityEntity -> {
                updateMapsIdName(map, properties.name, properties.name)
            }

            is CountryModel -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is DistrictEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is ProgramEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is ShasthyaKormiEntity -> {
                val displayName = if (properties.firstName.isNotBlank() && properties.lastName.isNotBlank()) {
                    "${properties.firstName} ${properties.lastName}"
                } else {
                    properties.firstName
                }
                updateMapsIdName(map, properties.id, displayName)
            }

            is ShasthyaShebikaEntity -> {
                // Format as "[ssid] [name]" if ssId exists, otherwise just use name
                val displayName = if (!properties.ssId.isNullOrBlank()) {
                    "${properties.ssId} - ${properties.name}"
                } else {
                    properties.name
                }
                updateMapsIdName(map, properties.id, displayName)
            }

            is HouseholdMemberEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is SignsAndSymptomsEntity -> {
                updateMapsIdName(map, properties.value ?: "", properties.symptom, properties.displayValue)
            }
        }
    }

    private fun updateMapsIdName(
        map: HashMap<String, Any>,
        id: Any,
        name: String,
        cultureValue: String? = null,
    ) {
        map[DefinedParams.ID] = id
        map[DefinedParams.NAME] = name
        cultureValue?.let {
            map[DefinedParams.CULTURE_VALUE] = cultureValue
        }
    }

    fun mapToSignsAndSymptomsEntity(
        optionsList: List<Map<String, Any>>?,
        type: String? = null,
    ): ArrayList<SignsAndSymptomsEntity> {
        if (optionsList.isNullOrEmpty()) {
            return arrayListOf()
        }
        val outputList = arrayListOf<SignsAndSymptomsEntity>()
        optionsList.forEachIndexed { index, it ->
            outputList.add(
                SignsAndSymptomsEntity(
                    _id = (it[DefinedParams.ID] as? Number)?.toLong() ?: index.toLong(),
                    symptom = it[DefinedParams.NAME] as String,
                    type = it[DefinedParams.type] as? String ?: type,
                    value = it[DefinedParams.Value] as? String,
                    displayOrder = it[DefinedParams.DISPLAY_ORDER] as? Int,
                    displayValue = it[DefinedParams.CULTURE_VALUE] as? String,
                ),
            )
        }
        return outputList
    }
}
