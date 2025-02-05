package com.medtroniclabs.opensource.ui.household.summary

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils.getAgeFromDOB
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.SpinnerDropDownBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.model.MemberDetailsSpinnerModel

class EditMemberSpinnerAdapter(context: Context, val translate: Boolean = false) :
    ArrayAdapter<String>(context, R.layout.spinner_drop_down_item) {

    var itemList = ArrayList<MemberDetailsSpinnerModel>()

    fun setData(listItems: ArrayList<MemberDetailsSpinnerModel>) {
        itemList = listItems
    }

    fun getData(position: Int): MemberDetailsSpinnerModel? {
        return if (position < itemList.size) itemList[position] else null
    }

    override fun getCount(): Int = itemList.size

    override fun getItem(position: Int): String {
            return constructListItem(itemList[position])
    }

    private fun constructListItem(itemList: MemberDetailsSpinnerModel): String {
        return if (itemList.id == DefinedParams.DefaultSelectID) {
            itemList.name
        } else {
            val genderPrefix = itemList.gender?.get(0).toString().capitalizeFirstChar()
            context.getString(
                R.string.household_summary_member_info,
                itemList.name,
                getAgeFromDOB(itemList.dob, context),
                genderPrefix
            )
        }
    }


    override fun getItemId(position: Int): Long = position.toLong()

    private fun createView(position: Int, viewGroup: ViewGroup?): View {
        val binding = SpinnerDropDownBinding.inflate(
            LayoutInflater.from(viewGroup?.context)
        )
        binding.tvTitle.text = getItem(position)
        return binding.root
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getDropDownView(position: Int, mView: View?, parentGroup: ViewGroup): View {
        return createView(position, parentGroup)
    }

    fun getIndexOfItem(id: Long): Int {
        return itemList.indexOfFirst { it.id == id }
    }

    fun getIndexOfItemByName(name: String): Int {
        return itemList.indexOfFirst { it.name == name }
    }
}