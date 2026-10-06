package com.mswdo.kalayaan.faced.data

import com.mswdo.kalayaan.faced.data.entity.*

/**
 * Populates the database on first launch.
 */
object SeedData {

    suspend fun populate(db: AppDatabase) {
        if (db.evacCenterDao().count() > 0) return // already seeded

        // 5 Evacuation Centers
        db.evacCenterDao().insertAll(
            listOf(
                EvacCenterEntity(name = "Kalayaan Central Gym", location = "Brgy. San Juan", capacity = 150),
                EvacCenterEntity(name = "Longos Elementary School", location = "Brgy. Longos", capacity = 100),
                EvacCenterEntity(name = "San Antonio Multi-Purpose Hall", location = "Brgy. San Antonio", capacity = 80),
                EvacCenterEntity(name = "San Roque Community Center", location = "Brgy. San Roque", capacity = 120),
                EvacCenterEntity(name = "Magalolon Evacuation Tent", location = "Brgy. Magalolon", capacity = 50)
            )
        )

        // 10 Inventory Items
        db.inventoryDao().insertAll(
            listOf(
                InventoryItemEntity(code = "FFP-DSWD", name = "Family Food Packs (FFP)", category = "Food Rations", unit = "pack", qty = 420, lowStockThreshold = 50),
                InventoryItemEntity(code = "HYG-STD", name = "Hygiene Kits", category = "Hygiene & Sanitation", unit = "kit", qty = 150, lowStockThreshold = 30),
                InventoryItemEntity(code = "SLP-BED", name = "Sleeping Kits & Modular Beds", category = "Bedding & Shelter", unit = "set", qty = 85, lowStockThreshold = 20),
                InventoryItemEntity(code = "WTR-CAN", name = "Jerry Cans (Water)", category = "Water & Supplies", unit = "can", qty = 50, lowStockThreshold = 20),
                InventoryItemEntity(code = "BTL-WTR", name = "Bottled Water (1L)", category = "Water & Supplies", unit = "bottle", qty = 500, lowStockThreshold = 100),
                InventoryItemEntity(code = "CANNED-BOX", name = "Canned Goods Assortment", category = "Food Rations", unit = "box", qty = 120, lowStockThreshold = 20),
                InventoryItemEntity(code = "MED-FAK", name = "First Aid Kits", category = "Medical Supplies", unit = "kit", qty = 60, lowStockThreshold = 10),
                InventoryItemEntity(code = "SLP-BLK", name = "Blankets", category = "Bedding & Shelter", unit = "piece", qty = 200, lowStockThreshold = 40),
                InventoryItemEntity(code = "EQP-FLS", name = "Flashlights (+Batteries)", category = "Equipment", unit = "piece", qty = 75, lowStockThreshold = 15),
                InventoryItemEntity(code = "SHL-TARP", name = "Tarpaulins", category = "Bedding & Shelter", unit = "roll", qty = 30, lowStockThreshold = 5)
            )
        )

        // 5 FACED Forms
        val records = listOf(
            FacedRecordEntity(
                serial = "KL-2026-001", headName = "Danilo Ramos", barangay = "San Juan",
                evacCenterName = "Kalayaan Central Gym", disasterEvent = "Typhoon / Severe Storm", stormName = "Ondoy",
                civilStatus = "Married", sex = "Male", birthDate = "1982-04-12", age = 44,
                birthPlace = "Kalayaan, Laguna", occupation = "Fisherman", religion = "Roman Catholic",
                monthlyIncome = "5,000 to 10,000", idType = "PhilID", idNumber = "4125-9874-1234",
                contactNumber = "0917-889-1234", altContactNumber = "N/A", mothersMaidenName = "Santos",
                permanentAddress = "Purok 2, Brgy. San Juan", is4Ps = true,
                shelterDamage = ShelterDamage.TOTALLY_DAMAGED, status = EvacuationStatus.IN_CENTER,
                presentMembersCount = 5, isSynced = false, filedByUserId = 1
            ),
            FacedRecordEntity(
                serial = "KL-2026-002", headName = "Maria Lourdes Santos", barangay = "Longos",
                evacCenterName = "Longos Elementary School", disasterEvent = "Flash Flood / Inundation", stormName = "",
                civilStatus = "Married", sex = "Female", birthDate = "1988-08-25", age = 38,
                birthPlace = "Paete, Laguna", occupation = "Vendor", religion = "Roman Catholic",
                monthlyIncome = "5,000 to 10,000", idType = "Voter's ID", idNumber = "2204-5512",
                contactNumber = "0920-554-3210", altContactNumber = "", mothersMaidenName = "Cruz",
                permanentAddress = "Sitio Riverside, Longos", is4Ps = false,
                shelterDamage = ShelterDamage.PARTIALLY_DAMAGED, status = EvacuationStatus.IN_CENTER,
                presentMembersCount = 3, isSynced = false, filedByUserId = 1
            ),
            FacedRecordEntity(
                serial = "KL-2026-003", headName = "Jose Rizalino", barangay = "San Antonio",
                evacCenterName = "San Antonio Multi-Purpose Hall", disasterEvent = "Earthquake", stormName = "",
                civilStatus = "Single", sex = "Male", birthDate = "1995-12-30", age = 30,
                birthPlace = "Calamba, Laguna", occupation = "Teacher", religion = "Roman Catholic",
                monthlyIncome = "10,000 to 20,000", idType = "PhilID", idNumber = "1234-5678-9012",
                contactNumber = "0919-123-4567", altContactNumber = "", mothersMaidenName = "Mercado",
                permanentAddress = "Blk 4, Brgy. San Antonio", is4Ps = false,
                shelterDamage = ShelterDamage.NONE, status = EvacuationStatus.LEFT_CENTER,
                presentMembersCount = 0, isSynced = false, filedByUserId = 1
            ),
            FacedRecordEntity(
                serial = "KL-2026-004", headName = "Carmen Dela Cruz", barangay = "San Roque",
                evacCenterName = "San Roque Community Center", disasterEvent = "Fire Incident", stormName = "",
                civilStatus = "Widowed", sex = "Female", birthDate = "1950-02-14", age = 76,
                birthPlace = "Lumban, Laguna", occupation = "None", religion = "Christian",
                monthlyIncome = "Below 5,000", idType = "Senior Citizen ID", idNumber = "SC-2021-001",
                contactNumber = "0922-987-6543", altContactNumber = "", mothersMaidenName = "Reyes",
                permanentAddress = "Purok 1, Brgy. San Roque", is4Ps = true,
                shelterDamage = ShelterDamage.TOTALLY_DAMAGED, status = EvacuationStatus.IN_CENTER,
                presentMembersCount = 1, isSynced = false, filedByUserId = 1
            ),
            FacedRecordEntity(
                serial = "KL-2026-005", headName = "Robert Fernandez", barangay = "Magalolon",
                evacCenterName = "Magalolon Evacuation Tent", disasterEvent = "Landslide", stormName = "",
                civilStatus = "Married", sex = "Male", birthDate = "1980-07-07", age = 46,
                birthPlace = "Kalayaan, Laguna", occupation = "Farmer", religion = "Roman Catholic",
                monthlyIncome = "5,000 to 10,000", idType = "Barangay ID", idNumber = "BRGY-MAG-005",
                contactNumber = "0915-444-5555", altContactNumber = "", mothersMaidenName = "Garcia",
                permanentAddress = "Sitio Ibaba, Brgy. Magalolon", is4Ps = true,
                shelterDamage = ShelterDamage.PARTIALLY_DAMAGED, status = EvacuationStatus.IN_CENTER,
                presentMembersCount = 3, isSynced = false, filedByUserId = 1
            )
        )
        db.facedRecordDao().insertAll(records)

        val members = listOf(
            HouseholdMemberEntity(facedSerial = "KL-2026-001", name = "Elena Ramos", relation = "Spouse", sex = "Female", birthDate = "1984-06-15", age = 42, ageCategory = "Adult", education = "High School", occupation = "Housewife"),
            HouseholdMemberEntity(facedSerial = "KL-2026-001", name = "Jake Ramos", relation = "Son", sex = "Male", birthDate = "2010-09-20", age = 16, ageCategory = "Child/Minor", education = "High School", occupation = "Student"),
            HouseholdMemberEntity(facedSerial = "KL-2026-001", name = "Chloe Ramos", relation = "Daughter", sex = "Female", birthDate = "2024-02-10", age = 2, ageCategory = "Infant (0-2)", education = "None", occupation = "None"),
            HouseholdMemberEntity(facedSerial = "KL-2026-001", name = "Luz Ramos", relation = "Mother", sex = "Female", birthDate = "1958-03-05", age = 68, ageCategory = "Senior Citizen (60+)", education = "Elementary", occupation = "None"),
            
            HouseholdMemberEntity(facedSerial = "KL-2026-002", name = "Arthur Santos", relation = "Spouse", sex = "Male", birthDate = "1986-01-10", age = 40, ageCategory = "Adult", education = "College", occupation = "Carpenter"),
            HouseholdMemberEntity(facedSerial = "KL-2026-002", name = "Kenneth Santos", relation = "Son", sex = "Male", birthDate = "2018-05-14", age = 8, ageCategory = "Child/Minor", education = "Elementary", occupation = "Student", remarks = "Asthma"),
            
            HouseholdMemberEntity(facedSerial = "KL-2026-005", name = "Linda Fernandez", relation = "Spouse", sex = "Female", birthDate = "1983-09-12", age = 43, ageCategory = "Adult", education = "High School", occupation = "Housewife"),
            HouseholdMemberEntity(facedSerial = "KL-2026-005", name = "Robby Fernandez", relation = "Son", sex = "Male", birthDate = "2025-01-01", age = 1, ageCategory = "Infant (0-2)", education = "None", occupation = "None")
        )
        db.householdMemberDao().insertAll(members)

        val tags = listOf(
            VulnerableTagEntity(facedSerial = "KL-2026-001", personName = "Luz Ramos", vulnerabilityType = "Senior Citizen (60+)"),
            VulnerableTagEntity(facedSerial = "KL-2026-001", personName = "Elena Ramos", vulnerabilityType = "Lactating Mother"),
            VulnerableTagEntity(facedSerial = "KL-2026-001", personName = "Jake Ramos", vulnerabilityType = "Child/Minor"),
            VulnerableTagEntity(facedSerial = "KL-2026-001", personName = "Chloe Ramos", vulnerabilityType = "Child/Minor"),
            
            VulnerableTagEntity(facedSerial = "KL-2026-002", personName = "Arthur Santos", vulnerabilityType = "Person with Disability (PWD)"),
            VulnerableTagEntity(facedSerial = "KL-2026-002", personName = "Kenneth Santos", vulnerabilityType = "Child/Minor"),

            VulnerableTagEntity(facedSerial = "KL-2026-004", personName = "Carmen Dela Cruz", vulnerabilityType = "Senior Citizen (60+)"),
            VulnerableTagEntity(facedSerial = "KL-2026-004", personName = "Carmen Dela Cruz", vulnerabilityType = "Solo Parent"),

            VulnerableTagEntity(facedSerial = "KL-2026-005", personName = "Robby Fernandez", vulnerabilityType = "Child/Minor")
        )
        db.vulnerableTagDao().insertAll(tags)
    }
}
