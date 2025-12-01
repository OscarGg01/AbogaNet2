package com.example.aboganet2.data

data class EducationItem(
    val titulo: String = "",
    val universidad: String = "",
    val anio: String = ""
)

data class ExperienceItem(
    val puesto: String = "",
    val empresa: String = "",
    val periodo: String = ""
)

data class LawyerProfile(
    val disponibilidad: Boolean = false,
    val costoConsulta: Double? = null,
    val especialidad: String = "",
    val logros: String = "",
    val educacion: List<EducationItem> = emptyList(),
    val experiencia: List<ExperienceItem> = emptyList(),
    val diasAtencion: List<String> = emptyList(),
    val horarioInicio: String = "",
    val horarioFin: String = ""
)

data class FullLawyerProfile(
    val basicInfo: User? = null,
    val professionalInfo: LawyerProfile? = null
)
