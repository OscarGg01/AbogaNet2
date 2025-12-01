package com.example.aboganet2.ui.screens

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.FullLawyerProfile
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
private fun generateTimeSlots(start: String, end: String): List<LocalTime> {
    if (start.isBlank() || end.isBlank()) return emptyList()
    val slots = mutableListOf<LocalTime>()
    // Este formateador espera "AM" o "PM", que es lo que ahora guardamos.
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    try {
        var currentTime = LocalTime.parse(start, formatter)
        val endTime = LocalTime.parse(end, formatter)
        while (currentTime.isBefore(endTime)) {
            slots.add(currentTime)
            currentTime = currentTime.plusMinutes(30)
        }
    } catch (e: Exception) {
        // Si algo falla, imprime el error en el Logcat para poder depurarlo.
        Log.e("generateTimeSlots", "Error al parsear las horas: '$start', '$end'", e)
        return emptyList()
    }
    return slots
}

@RequiresApi(Build.VERSION_CODES.O)
private val dayNameToDayOfWeek = mapOf(
    "Lun" to DayOfWeek.MONDAY, "Mar" to DayOfWeek.TUESDAY, "Mié" to DayOfWeek.WEDNESDAY,
    "Jue" to DayOfWeek.THURSDAY, "Vie" to DayOfWeek.FRIDAY, "Sáb" to DayOfWeek.SATURDAY,
    "Dom" to DayOfWeek.SUNDAY
)

// --- COMPONENTES DE LA UI DEL CALENDARIO ---
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Day(day: CalendarDay, isSelected: Boolean, isEnabled: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else Color.LightGray,
                shape = CircleShape
            )
            .clickable(enabled = isEnabled, onClick = { onClick(day) }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            color = when {
                isSelected -> Color.White
                !isEnabled -> Color.LightGray
                else -> LocalContentColor.current
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarHeader(currentMonth: YearMonth, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ChevronLeft, "Mes anterior") }
        Text(
            text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() } + " " + currentMonth.year,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, "Mes siguiente") }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val daysOfWeek = DayOfWeek.values()
        val orderedDays = daysOfWeek.drop(1) + daysOfWeek.first()
        for (dayOfWeek in orderedDays) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).replaceFirstChar { it.uppercase() },
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// --- PANTALLA PRINCIPAL ACTUALIZADA ---
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    lawyerId: String,
    lawyerName: String,
    cost: Float,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onContinue: (String, String, Float, Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var lawyerFullProfile by remember { mutableStateOf<FullLawyerProfile?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }

    val workingDaysOfWeek = remember(lawyerFullProfile) {
        lawyerFullProfile?.professionalInfo?.diasAtencion?.mapNotNull { dayNameToDayOfWeek[it] } ?: emptyList()
    }

    // **CAMBIO CLAVE**: Ahora `timeSlots` es una lista que se recalcula cuando `selectedDate` cambia.
    val timeSlots = remember(selectedDate, lawyerFullProfile) {
        if (selectedDate == null || lawyerFullProfile == null) {
            emptyList()
        } else {
            val allSlots = generateTimeSlots(
                lawyerFullProfile?.professionalInfo?.horarioInicio ?: "",
                lawyerFullProfile?.professionalInfo?.horarioFin ?: ""
            )

            // Si la fecha seleccionada es hoy, filtramos las horas que ya pasaron.
            if (selectedDate!!.isEqual(LocalDate.now())) {
                val now = LocalTime.now()
                allSlots.filter { it.isAfter(now) }
            } else {
                // Si es un día futuro, mostramos todas las horas.
                allSlots
            }
        }
    }

    LaunchedEffect(lawyerId) {
        authViewModel.getLawyerById(lawyerId) { profile ->
            lawyerFullProfile = profile
        }
    }

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val firstDayOfWeek = remember { DayOfWeek.MONDAY }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Horario", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background) // Opcional: para que no sea transparente
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedDate != null && selectedTime != null) {
                            val zoneId = ZoneId.systemDefault()
                            val timestamp = selectedDate!!.atTime(selectedTime!!).atZone(zoneId).toInstant().toEpochMilli()
                            onContinue(lawyerName, lawyerId, cost, timestamp)
                        } else {
                            Toast.makeText(context, "Por favor, seleccione un día y una hora", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedDate != null && selectedTime != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Continuar", fontSize = 18.sp)
                }
            }
        }
    ) { paddingValues ->
        if (lawyerFullProfile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val visibleMonth = calendarState.firstVisibleMonth
                CalendarHeader(
                    currentMonth = visibleMonth.yearMonth,
                    onPrevMonth = { coroutineScope.launch { calendarState.animateScrollToMonth(visibleMonth.yearMonth.minusMonths(1)) } },
                    onNextMonth = { coroutineScope.launch { calendarState.animateScrollToMonth(visibleMonth.yearMonth.plusMonths(1)) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DaysOfWeekHeader()
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalCalendar(
                    state = calendarState,
                    dayContent = { day ->
                        val isWorkingDay = day.date.dayOfWeek in workingDaysOfWeek
                        val isTodayOrFuture = !day.date.isBefore(LocalDate.now())
                        val isEnabled = isWorkingDay && isTodayOrFuture

                        Day(day = day, isSelected = selectedDate == day.date, isEnabled = isEnabled) {
                            selectedDate = it.date
                            selectedTime = null
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (selectedDate != null) {
                    Text("Horas Disponibles", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (timeSlots.isEmpty()) {
                        Text("No hay horas disponibles para este día.", color = Color.Gray)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(timeSlots) { time ->
                                val isSelected = time == selectedTime
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedTime = time }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = time.format(DateTimeFormatter.ofPattern("hh:mm a")),
                                        color = if (isSelected) Color.White else LocalContentColor.current,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}