package com.example.aboganet2.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.Consultation
import com.example.aboganet2.data.User
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LawyerAgendaScreen(authViewModel: AuthViewModel = viewModel()) {
    // CORRECCIÓN 1: Observar el StateFlow directamente desde el ViewModel
    val allConsultations by authViewModel.lawyerConsultations.collectAsState()

    // Estado para la fecha que el abogado seleccione en el calendario.
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // Ya no necesitamos el LaunchedEffect para cargar los datos,
    // el ViewModel ya lo está haciendo.

    // Creamos un conjunto (Set) de las fechas que tienen al menos una consulta.
    val consultationDates = remember(allConsultations) {
        allConsultations.mapNotNull { (consultation, _) ->
            consultation.appointmentTimestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
        }.toSet()
    }

    // Filtramos la lista de consultas para mostrar solo las del día seleccionado.
    val consultationsForSelectedDate = remember(selectedDate, allConsultations) {
        if (selectedDate == null) {
            emptyList()
        } else {
            allConsultations.filter { (consultation, _) ->
                val consultationDate = consultation.appointmentTimestamp?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                consultationDate?.isEqual(selectedDate) == true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mi Agenda", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // --- CALENDARIO ---
        AgendaCalendarView(
            consultationDates = consultationDates,
            onDateSelected = { date ->
                selectedDate = if (selectedDate == date) null else date
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- LISTA DE CONSULTAS DEL DÍA SELECCIONADO ---
        if (selectedDate != null) {
            val formattedDate = selectedDate!!.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("es", "ES")))
            Text(
                "Consultas para el $formattedDate",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (consultationsForSelectedDate.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(consultationsForSelectedDate) { (consultation, client) ->
                        if (client != null) {
                            ConsultationCard(consultation, client)
                        }
                    }
                }
            }  else {
                Text("No hay consultas para este día.")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AgendaCalendarView(
    consultationDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = java.time.DayOfWeek.MONDAY
    )

    HorizontalCalendar(
        state = calendarState,
        dayContent = { day ->
            val hasConsultation = day.date in consultationDates
            AgendaDay(
                day = day,
                isSelected = selectedDate == day.date,
                hasConsultation = hasConsultation,
                onClick = {
                    if (day.position == DayPosition.MonthDate) {
                        selectedDate = it.date
                        onDateSelected(it.date)
                    }
                }
            )
        },
        monthHeader = { month ->
            CalendarHeader(
                currentMonth = month.yearMonth,
                onPrevMonth = { coroutineScope.launch { calendarState.animateScrollToMonth(month.yearMonth.minusMonths(1)) } },
                onNextMonth = { coroutineScope.launch { calendarState.animateScrollToMonth(month.yearMonth.plusMonths(1)) } }
            )
        }
    )
    DaysOfWeekHeader()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AgendaDay(
    day: CalendarDay,
    isSelected: Boolean,
    hasConsultation: Boolean,
    onClick: (CalendarDay) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
        )
        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (day.position == DayPosition.MonthDate) LocalContentColor.current else Color.Gray
        )
        if (hasConsultation && day.position == DayPosition.MonthDate) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ConsultationCard(consultation: Consultation, client: User) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    val consultationTime = consultation.appointmentTimestamp?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.toLocalTime()
        ?.format(timeFormatter) ?: "Hora no definida"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = consultation.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // CORRECCIÓN 2: Usar el campo 'name' en lugar de 'nombre'
            Text(
                text = "Cliente: ${client.nombre}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hora: $consultationTime",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// NOTA: Asumo que los componentes CalendarHeader y DaysOfWeekHeader ya existen en tu proyecto
// y son accesibles desde este archivo. Si no lo son, deberás copiarlos aquí también.