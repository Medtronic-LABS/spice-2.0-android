package org.medtroniclabs.uhis.ui.household.summary

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils.getAgeFromDOB
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.SpinnerDropDownBinding
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.model.MemberDetailsSpinnerModel

class EditMemberSpinnerAdapter(context: Context, val translate: Boolean = false) :
    ArrayAdapter<String>(context, R.layout.spinner_drop_down_item) {
    var itemList = ArrayList<MemberDetailsSpinnerModel>()

    fun setData(listItems: ArrayList<MemberDetailsSpinnerModel>) {
        itemList = listItems
    }

    fun getData(position: Int): MemberDetailsSpinnerModel? = if (position < itemList.size) itemList[position] else null

    override fun getCount(): Int = itemList.size

    override fun getItem(position: Int): String = constructListItem(itemList[position])

    private fun constructListItem(itemList: MemberDetailsSpinnerModel): String =
        if (itemList.id == DefinedParams.DEFAULT_SELECT_ID) {
            itemList.name
        } else {
            val genderPrefix = itemList.gender
                ?.get(0)
                .toString()
                .capitalizeFirstChar()
            context.getString(
                R.string.household_summary_member_info,
                itemList.name,
                getAgeFromDOB(itemList.dob, context),
                genderPrefix,
            )
        }

    override fun getItemId(position: Int): Long = position.toLong()

    private fun createView(
        position: Int,
        viewGroup: ViewGroup?,
    ): View {
        val binding = SpinnerDropDownBinding.inflate(
            LayoutInflater.from(viewGroup?.context),
        )
        binding.tvTitle.text = getItem(position)
        return binding.root
    }

    override fun getItemViewType(position: Int): Int = position

    override fun getDropDownView(
        position: Int,
        mView: View?,
        parentGroup: ViewGroup,
    ): View = createView(position, parentGroup)

    fun getIndexOfItem(id: Long): Int = itemList.indexOfFirst { it.id == id }

    fun getIndexOfItemByName(name: String): Int = itemList.indexOfFirst { it.name == name }
}
