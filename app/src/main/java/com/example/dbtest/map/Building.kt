//colors;
//it - gray
//hm - orange
//tm - yellow
//bse - blue
//canteens kakasjay - green
//faculties - darker color depende no niya department
//library, fishfishes, courts, dean, hostel, student center, ssc - red
//parkingan - brown
//balay ti uttogs - pink
//reserve-able lang jay it buildings, dadduma macklick ngem info lang agparang
//pag joinen yunto man agijay buildings ta maifill colors tu langnen

package com.example.dbtest.map

import android.graphics.Color
import android.graphics.PointF

data class Building(
    val id: String,
    val label: String,
    val points: List<PointF>,
    val fillColor: Int? = null,
    val floor: Int = 1,
    val showOnOtherFloor: Boolean = true,
    val clickable: Boolean = true,
    val reservable: Boolean = false
)

//bot-right, bot-left, top-left, top-right
object CampusBuildings {

    private val IT_GRAY = Color.argb(170, 105, 105, 105)
    private val HM_ORANGE = Color.argb(170, 255, 121, 0)
    private val TM_YELLOW = Color.argb(170, 255, 235, 59)
    private val BSE_BLUE = Color.argb(170, 33, 150, 243)
    private val CANTEEN_GREEN = Color.argb(170, 76, 175, 80)
    private val LANDMARK_RED = Color.argb(170, 244, 67, 54)
    private val PARKING_AREA_COLOR = Color.rgb(235, 231, 191)
    private val UNCOLORED_BROWN = Color.argb(170, 121, 92, 52)
    private val GUARD_PINK = Color.argb(170, 255, 192, 203)

    val OTHER_BUILDINGS: List<Building> = listOf(
        Building("building_1", "CR", pts(247,156, 226,156, 226,132, 247,132), fillColor = LANDMARK_RED),
        Building("building_2", "CR", pts(437,99, 463,98, 464,134, 438,135), fillColor = LANDMARK_RED),
        Building("building_3a", "SSC Building", pts(556,89, 474,88, 473,128, 558,129), fillColor = LANDMARK_RED),
        Building("building_3b", "Chapel", pts(594,45, 549,51, 560,108, 564,125, 608,118), fillColor = UNCOLORED_BROWN),
        Building("building_4a", "Training Center - Room 1", pts(619,74, 618,42, 664,40, 665,72), fillColor = UNCOLORED_BROWN, floor = 2),
        Building("building_4a_gf", "Training Center - Room 1", pts(619,74, 618,42, 664,40, 665,72), fillColor = UNCOLORED_BROWN),
        Building("building_4b", "Training Center - Room 2", pts(665,72, 664,40, 711,39, 712,70), fillColor = UNCOLORED_BROWN, floor = 2),
        Building("building_4b_gf", "Training Center - Room 2", pts(665,72, 664,40, 711,39, 712,70), fillColor = UNCOLORED_BROWN),
        Building("building_4c", "Training Center - Room 3", pts(712,70, 711,39, 757,37, 758,68), fillColor = UNCOLORED_BROWN, floor = 2),
        Building("building_4c_gf", "Training Center - Room 3", pts(712,70, 711,39, 757,37, 758,68), fillColor = UNCOLORED_BROWN),
        Building("building_4d", "Training Center - Storage 1", pts(621,147, 619,74, 659,72, 662,146), fillColor = UNCOLORED_BROWN, floor = 2, clickable = false),
        Building("building_4d_gf", "Training Center - Storage 1", pts(621,147, 619,74, 659,72, 662,146), fillColor = UNCOLORED_BROWN, clickable = false),
        Building("building_4e", "Training Center - Storage 2", pts(760,112, 729,113, 728,70, 758,68), fillColor = UNCOLORED_BROWN, floor = 2, clickable = false),
        Building("building_4e_gf", "Training Center - Storage 2", pts(760,112, 729,113, 728,70, 758,68), fillColor = UNCOLORED_BROWN, clickable = false),
        Building("building_4f", "Training Center - Room 4", pts(661,114, 659,72, 728,70, 729,112), fillColor = UNCOLORED_BROWN),
        Building("building_4g", "Training Center - Room 5", pts(662,146, 661,114, 695,113, 696,144), fillColor = UNCOLORED_BROWN, floor = 2),
        Building("building_4g_gf", "Training Center - Room 5", pts(662,146, 661,114, 695,113, 696,144), fillColor = UNCOLORED_BROWN),
        Building("building_4h", "Training Center - Room 6", pts(696,144, 695,113, 729,112, 730,143), fillColor = UNCOLORED_BROWN, floor = 2),
        Building("building_4h_gf", "Training Center - Room 6", pts(696,144, 695,113, 729,112, 730,143), fillColor = UNCOLORED_BROWN),
        Building("building_4i", "Training Center - Room 7", pts(760,142, 729,143, 729,112, 760,112), fillColor = UNCOLORED_BROWN, floor = 2),
        Building("building_4i_gf", "Training Center - Room 7", pts(760,142, 729,143, 729,112, 760,112), fillColor = UNCOLORED_BROWN),
        Building("building_5", "Fishery", pts(993,147, 909,147, 909,44, 993,44), fillColor = LANDMARK_RED),
        Building("building_8", "Registrar", pts(928,218, 867,218, 867,147, 928,147), fillColor = UNCOLORED_BROWN),
        Building("building_9", "Parking Area", pts(1263,222, 1088,222, 1088,107, 1263,107), fillColor = PARKING_AREA_COLOR),
        Building("building_10", "Gray gym", pts(134,327, 17,327, 17,158, 134,158), fillColor = UNCOLORED_BROWN),
        Building("building_11", "Open Court", pts(365,346, 265,346, 265,194, 365,194), fillColor = LANDMARK_RED),
        Building("building_14", "Canteen 2", pts(795,270, 757,269, 758,241, 796,242), fillColor = CANTEEN_GREEN),
        Building("building_15", "Canteen 3", pts(835,266, 807,266, 807,246, 835,246), fillColor = CANTEEN_GREEN),
        Building("building_16", "Printing Services", pts(860,264, 835,264, 835,245, 860,245), fillColor = CANTEEN_GREEN),
        Building("building_17", "Canteen 4", pts(898,274, 873,274, 873,255, 898,255), fillColor = CANTEEN_GREEN),
        Building("building_27", "HM Building", pts(975,313, 930,313, 930,253, 975,253), fillColor = HM_ORANGE, reservable = true),
        Building("building_20", "School Van Parking Area", pts(750,356, 713,356, 713,325, 750,325), fillColor = PARKING_AREA_COLOR),
        Building("building_21", "Canteen 1", pts(814,330, 778,330, 778,283, 814,283), fillColor = CANTEEN_GREEN),
        Building("building_22", "Hostel", pts(1002,335, 999,271, 1037,270, 1040,334), fillColor = LANDMARK_RED, floor = 2),
        Building("building_22_gf", "Hostel", pts(1002,335, 999,271, 1037,270, 1040,334), fillColor = LANDMARK_RED),
        Building("building_23", "Guard's House Main", pts(1206,383, 1178,383, 1178,356, 1206,356), fillColor = GUARD_PINK),
        Building("building_25", "Guard's House", pts(666,492, 646,492, 646,471, 666,471), fillColor = GUARD_PINK),
        Building("building_26", "Ancient", pts(763,464, 679,464, 679,402, 763,402), fillColor = UNCOLORED_BROWN),
    )

    val IT_BUILDINGS: List<Building> = listOf(
        Building("building_12a", "IT 1B Room", pts(427,218, 427,181, 473,181, 474,218), fillColor = IT_GRAY, reservable = true),
        Building("building_13a", "IT 1A Room", pts(538,198, 615,198, 615,229, 538,229), fillColor = IT_GRAY, floor = 2, reservable = true),
        Building("building_13b", "Computer Lab/IT4", pts(587,229, 615,229, 615,280, 587,280), fillColor = IT_GRAY, floor = 2, reservable = true),
        Building("building_13c", "Internet Room/IT3", pts(587,280, 615,280, 615,330, 587,330), fillColor = IT_GRAY, floor = 2, reservable = true),
        Building("building_13b_gf", "HM Faculty", pts(587,229, 615,229, 615,280, 587,280), fillColor = HM_ORANGE, reservable = false),
        Building("building_13c_gf", "Library", pts(587,280, 615,280, 615,330, 587,330), fillColor = LANDMARK_RED, reservable = false),
        Building("building_19a", "IT 2B Room", pts(533,373, 533,340, 570,338, 570,373), fillColor = IT_GRAY, reservable = true),
        Building("building_19b", "IT 2A Room", pts(570,372, 570,338, 610,338, 610,372), fillColor = IT_GRAY, reservable = true),
        Building("building_19c", "IT Faculty", pts(611,372, 611,347, 637,349, 639,371), fillColor = IT_GRAY),
    )

    val HM_BUILDINGS: List<Building> = listOf(
        Building("building_7", "HM Room 1", pts(590,180, 524,180, 524,150, 590,150), fillColor = HM_ORANGE, reservable = true),
        Building("building_12b", "HM Room 2", pts(427,255, 427,218, 474,218, 474,255), fillColor = HM_ORANGE, reservable = true),
        Building("building_12c", "HM Room 3", pts(427,293, 427,255, 474,255, 475,293), fillColor = HM_ORANGE, reservable = true),
        Building("building_12d", "HM Room 4", pts(427,330, 427,293, 475,293, 475,330), fillColor = HM_ORANGE, reservable = true),
        Building("building_13a_gf1", "HM Room 5", pts(538,198, 577,198, 577,229, 538,229), fillColor = HM_ORANGE, showOnOtherFloor = false, reservable = true),
        Building("building_13a_gf2", "HM Room 6", pts(577,198, 615,198, 615,229, 577,229), fillColor = HM_ORANGE, showOnOtherFloor = false, reservable = true),
        Building("building_12e", "Faculty", pts(427,368, 427,330, 475,330, 476,368), fillColor = HM_ORANGE),
        Building("building_24a_gf", "South - CR 1", pts(333,476, 308,476, 308,416, 333,416), fillColor = LANDMARK_RED),
        Building("building_24b_gf", "South - Room 1", pts(389,476, 333,476, 333,416, 389,416), fillColor = HM_ORANGE, reservable = true),
        Building("building_24c_gf", "South - Room 2", pts(445,476, 389,476, 389,416, 445,416), fillColor = HM_ORANGE, reservable = true),
        Building("building_24d_gf", "South - Room 3", pts(502,476, 445,476, 445,416, 502,416), fillColor = HM_ORANGE, reservable = true),
        Building("building_24e_gf", "South - Room 4", pts(558,476, 502,476, 502,416, 558,416), fillColor = HM_ORANGE, reservable = true),
        Building("building_24f_gf", "South - CR 2", pts(583,476, 558,476, 558,416, 583,416), fillColor = LANDMARK_RED),
    )

    val TM_BUILDINGS: List<Building> = listOf(
        Building("building_6a", "TM - Room 1", pts(283,176, 247,176, 247,131, 283,131), fillColor = TM_YELLOW, reservable = true),
        Building("building_6b", "TM - Room 2", pts(325,176, 283,176, 283,131, 325,131), fillColor = TM_YELLOW, reservable = true),
        Building("building_6c", "TM - Room 3", pts(368,176, 325,176, 325,131, 368,131), fillColor = TM_YELLOW, reservable = true),
        Building("building_18a", "TM - Room 4", pts(163,294, 162,255, 206,255, 207,294), fillColor = TM_YELLOW, reservable = true),
        Building("building_24a", "South - CR 1", pts(333,476, 308,476, 308,416, 333,416), fillColor = LANDMARK_RED, floor = 2),
        Building("building_24b", "South - Room 1", pts(389,476, 333,476, 333,416, 389,416), fillColor = TM_YELLOW, floor = 2, reservable = true),
        Building("building_24c", "South - Room 2", pts(445,476, 389,476, 389,416, 445,416), fillColor = TM_YELLOW, floor = 2, reservable = true),
        Building("building_24d", "South - Room 3", pts(502,476, 445,476, 445,416, 502,416), fillColor = TM_YELLOW, floor = 2, reservable = true),
        Building("building_24e", "South - Room 4", pts(558,476, 502,476, 502,416, 558,416), fillColor = TM_YELLOW, floor = 2, reservable = true),
        Building("building_24f", "South - CR 2", pts(583,476, 558,476, 558,416, 583,416), fillColor = LANDMARK_RED, floor = 2),
    )

    val BSE_BUILDINGS: List<Building> = listOf(
        Building("building_18b", "BSE - Room 5", pts(163,334, 163,294, 207,294, 208,334), fillColor = BSE_BLUE, reservable = true),
        Building("building_18c", "BSE - Room 6", pts(164,373, 163,334, 208,334, 209,373), fillColor = BSE_BLUE, reservable = true),
    )

    val ALL: List<Building> = OTHER_BUILDINGS + IT_BUILDINGS + HM_BUILDINGS + TM_BUILDINGS + BSE_BUILDINGS
    val GROUND_FLOOR: List<Building> = ALL.filter { it.floor != 2 }
    val SECOND_FLOOR: List<Building> = ALL.filter { it.floor == 2 }
}

// ---- 3D support -------------------------------------------------------
// Height of one storey in design-space units (design space is ~1284 wide).
const val FLOOR_HEIGHT = 30f

// Ground-level surfaces that should be painted flat instead of extruded.
private val FLAT_IDS = setOf("building_9", "building_11", "building_20")

// floor = 2 pieces sit on top of their "_gf" twins.
val Building.baseZ: Float get() = if (floor == 2) FLOOR_HEIGHT else 0f
val Building.topZ: Float get() = if (id in FLAT_IDS) 0f else baseZ + FLOOR_HEIGHT

private fun pts(vararg v: Int): List<PointF> {
    val list = mutableListOf<PointF>()
    var i = 0
    while (i < v.size) {
        list.add(PointF(v[i].toFloat(), v[i + 1].toFloat()))
        i += 2
    }
    return list
}