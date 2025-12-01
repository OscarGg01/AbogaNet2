package com.example.aboganet2.ui.screens

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.google.firebase.Timestamp
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
    var bookedAppointments by remember { mutableStateOf<List<Timestamp>>(emptyList()) }

    val workingDaysOfWeek = remember(lawyerFullProfile) {
        lawyerFullProfile?.professionalInfo?.diasAtencion?.mapNotNull { dayNameToDayOfWeek[it] } ?: emptyList()
    }

    LaunchedEffect(lawyerId) {
        coroutineScope.launch {
            authViewModel.getLawyerById(lawyerId) { profile ->
                lawyerFullProfile = profile
            }
            val appointments = authViewModel.getBookedAppointments(lawyerId)
            bookedAppointments = appointments
        }
    }

    val timeSlots = remember(selectedDate, lawyerFullProfile, bookedAppointments) {
        if (selectedDate == null || lawyerFullProfile == null) {
            emptyList()
        } else {
            val allSlots = generateTimeSlots(
                lawyerFullProfile?.professionalInfo?.horarioInicio ?: "",
                lawyerFullProfile?.professionalInfo?.horarioFin ?: ""
            )
            val bookedSlotsForSelectedDate = bookedAppointments.mapNotNull { timestamp ->
                val zonedDateTime = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault())
                if (zonedDateTime.toLocalDate().isEqual(selectedDate)) {
                    zonedDateTime.toLocalTime()
                } else {
                    null
                }
            }.toSet()
            val availableSlots = allSlots.filter { it !in bookedSlotsForSelectedDate }
            if (selectedDate!!.isEqual(LocalDate.now())) {
                val now = LocalTime.now()
                availableSlots.filter { it.isAfter(now) }
            } else {
                availableSlots
            }
        }
    }

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
                    .background(MaterialTheme.colorScheme.background)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Componente del calendario que ahora recibe los días laborables
            CalendarView(
                workingDays = workingDaysOfWeek,
                onDateSelected = { date ->
                    selectedDate = date
                    selectedTime = null
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedDate != null) {
                Text(
                    "Horas disponibles para ${selectedDate?.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (timeSlots.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(timeSlots) { time ->
                            val isSelected = time == selectedTime
                            TimeSlotItem(
                                time = time,
                                isSelected = isSelected,
                                onTimeSelected = { selectedTime = it }
                            )
                        }
                    }
                } else {
                    Text("No hay horas disponibles para este día.")
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarView(
    workingDays: List<DayOfWeek>,
    onDateSelected: (LocalDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

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

    HorizontalCalendar(
        modifier = Modifier.padding(vertical = 16.dp),
        state = calendarState,
        dayContent = { day ->
            val isWorkingDay = day.date.dayOfWeek in workingDays
            val isEnabled = day.date.isAfter(LocalDate.now().minusDays(1)) && isWorkingDay
            Day(
                day = day,
                isSelected = selectedDate == day.date,
                isEnabled = isEnabled,
                onClick = {
                    if (isEnabled) {
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
fun TimeSlotItem(time: LocalTime, isSelected: Boolean, onTimeSelected: (LocalTime) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                RoundedCornerShape(8.dp)
            )
            .clickable { onTimeSelected(time) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time.format(formatter),
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 14.sp
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun generateTimeSlots(start: String, end: String): List<LocalTime> {
    if (start.isBlank() || end.isBlank()) return emptyList()
    val slots = mutableListOf<LocalTime>()
    val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    try {
        var currentTime = LocalTime.parse(start, formatter)
        val endTime = LocalTime.parse(end, formatter)
        while (currentTime.isBefore(endTime)) {
            slots.add(currentTime)
            currentTime = currentTime.plusMinutes(30)
        }
    } catch (e: Exception) {
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