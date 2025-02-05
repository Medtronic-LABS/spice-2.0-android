package com.medtroniclabs.opensource.common

import com.medtroniclabs.opensource.data.CountryModel
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.data.ProgramEntity
import com.medtroniclabs.opensource.db.entity.ChiefDomEntity
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.DistrictEntity
import com.medtroniclabs.opensource.db.entity.VillageEntity

object EntityMapper {

    fun getResultSpinnerMapList(data: LocalSpinnerResponse): ArrayList<Map<String, Any>> {
        if (data.response is List<*>) {
            return ArrayList(data.response.map { properties ->
                val map = HashMap<String, Any>()
                mapsIdName(properties, map)
                map
            })
        }
        return ArrayList()
    }

    private fun mapsIdName(properties: Any?, map: HashMap<String, Any>) {
        when (properties) {
            is VillageEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }
            is HealthFacilityEntity -> {
                updateMapsIdName(map,properties.name,properties.name)
            }
            is CountryModel -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is ChiefDomEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is DistrictEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }

            is ProgramEntity -> {
                updateMapsIdName(map, properties.id, properties.name)
            }
        }
    }

    private fun updateMapsIdName(map: HashMap<String, Any>, id: Any, name: String) {
        map[DefinedParams.ID] = id
        map[DefinedParams.NAME] = name
    }

}