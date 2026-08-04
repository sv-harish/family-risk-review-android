package com.familyriskreview.feature.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.familyriskreview.core.designsystem.component.FrrPrimaryButton
import com.familyriskreview.core.designsystem.component.FrrSecondaryButton
import com.familyriskreview.core.designsystem.component.FrrStableTextField
import com.familyriskreview.core.designsystem.component.FrrTextAction
import com.familyriskreview.core.designsystem.theme.FrrRadius
import com.familyriskreview.core.designsystem.theme.FrrSpacing
import com.familyriskreview.core.designsystem.theme.FrrTypography
import com.familyriskreview.core.designsystem.theme.frrColors
import com.familyriskreview.core.model.ContributionStatus
import com.familyriskreview.core.model.DependencyStatus
import com.familyriskreview.core.model.FamilyMemberType
import com.familyriskreview.core.model.HouseholdMember
import com.familyriskreview.core.model.validation.HouseholdRules
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HouseholdSupportMapScreen(
    state: ReviewUiState,
    onAddMember: (FamilyMemberType) -> Unit,
    onUpdateMember: (HouseholdMember) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetFocus: (String) -> Unit,
    onSelectMember: (String) -> Unit,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val focusedId = state.review?.focusedIncomeContributorId
    val selected = state.members.find { it.id == state.selectedMemberId }
        ?: state.members.find { it.id == focusedId }
        ?: state.members.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        Text(text = stringResource(R.string.household_title), style = FrrTypography.headlineMedium, color = colors.onSurface)
        Text(
            text = stringResource(R.string.household_subtitle),
            style = FrrTypography.bodyLarge,
            color = colors.mutedText,
            modifier = Modifier.padding(top = FrrSpacing.xs, bottom = FrrSpacing.md),
        )
        state.validationIssues.forEach { issue ->
            Text(
                text = issue.message,
                style = FrrTypography.bodyMedium,
                color = if (issue.severity.name == "WARNING") colors.caution else colors.blockingError,
            )
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val twoPane = maxWidth >= 840.dp
            if (twoPane) {
                Row(Modifier.fillMaxSize()) {
                    HouseholdMapVisual(state.members, focusedId, selected?.id, onSelectMember,
                        Modifier.weight(0.48f).fillMaxHeight().padding(end = FrrSpacing.md))
                    HouseholdMemberEditor(selected, focusedId, onUpdateMember, onRemoveMember, onSetFocus,
                        Modifier.weight(0.52f).fillMaxHeight().verticalScroll(rememberScrollState()))
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    HouseholdMapVisual(state.members, focusedId, selected?.id, onSelectMember,
                        Modifier.fillMaxWidth().height(280.dp))
                    Spacer(Modifier.height(FrrSpacing.md))
                    HouseholdMemberEditor(selected, focusedId, onUpdateMember, onRemoveMember, onSetFocus, Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(FrrSpacing.sm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FrrSpacing.xs), verticalArrangement = Arrangement.spacedBy(FrrSpacing.xs)) {
            AddChip(stringResource(R.string.household_add_self)) { onAddMember(FamilyMemberType.SELF) }
            AddChip(stringResource(R.string.household_add_spouse)) { onAddMember(FamilyMemberType.SPOUSE_OR_PARTNER) }
            AddChip(stringResource(R.string.household_add_child)) { onAddMember(FamilyMemberType.CHILD) }
            AddChip(stringResource(R.string.household_add_parent)) { onAddMember(FamilyMemberType.PARENT) }
            AddChip(stringResource(R.string.household_add_other)) { onAddMember(FamilyMemberType.OTHER_DEPENDANT) }
        }
        Spacer(Modifier.height(FrrSpacing.md))
        FrrPrimaryButton(stringResource(R.string.review_continue), onAdvance, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddChip(label: String, onClick: () -> Unit) {
    val colors = frrColors()
    Text(
        text = label,
        style = FrrTypography.labelLarge,
        color = colors.primaryAction,
        modifier = Modifier.clip(RoundedCornerShape(FrrRadius.sm))
            .border(1.dp, colors.outline, RoundedCornerShape(FrrRadius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = FrrSpacing.sm, vertical = FrrSpacing.xs),
    )
}

@Composable
private fun HouseholdMapVisual(
    members: List<HouseholdMember>,
    focusedId: String?,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    val focus = members.find { it.id == focusedId } ?: members.firstOrNull()
    val others = members.filter { it.id != focus?.id }
    val focusLabel = focus?.displayLabel?.takeIf { it.isNotBlank() }
        ?: focus?.let { relationshipShort(it.relationship) }
        ?: stringResource(R.string.household_unlabelled)
    val mapDescription = stringResource(R.string.household_map_a11y, members.size, focusLabel)

    Box(
        modifier = modifier.background(colors.elevatedSurface).semantics { contentDescription = mapDescription }.padding(FrrSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        if (members.isEmpty()) {
            Text(text = stringResource(R.string.household_empty_hint), style = FrrTypography.bodyLarge,
                color = colors.mutedText, textAlign = TextAlign.Center)
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val cx = constraints.maxWidth / 2f
                val cy = constraints.maxHeight / 2f
                val orbit = with(density) { minOf(maxWidth, maxHeight).toPx() * 0.32f }
                Canvas(Modifier.fillMaxSize()) {
                    if (others.isNotEmpty()) {
                        drawCircle(colors.outline.copy(alpha = 0.45f), orbit, Offset(cx, cy), style = Stroke(2.dp.toPx()))
                    }
                    others.forEachIndexed { index, _ ->
                        val angle = (2.0 * Math.PI * index / others.size) - Math.PI / 2
                        drawLine(
                            colors.outline.copy(alpha = 0.55f),
                            Offset(cx, cy),
                            Offset(cx + (orbit * cos(angle)).toFloat(), cy + (orbit * sin(angle)).toFloat()),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
                focus?.let { member ->
                    MemberNode(member, true, member.id == selectedId, Modifier.align(Alignment.Center).size(88.dp)) {
                        onSelect(member.id)
                    }
                }
                others.forEachIndexed { index, member ->
                    val angle = (2.0 * Math.PI * index / others.size) - Math.PI / 2
                    val ox = (orbit * cos(angle)).toFloat()
                    val oy = (orbit * sin(angle)).toFloat()
                    MemberNode(member, false, member.id == selectedId,
                        Modifier.align(Alignment.Center).offset { IntOffset(ox.roundToInt(), oy.roundToInt()) }.size(72.dp)) {
                        onSelect(member.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberNode(
    member: HouseholdMember,
    focused: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = frrColors()
    val contributor = isIncomeContributor(member.contributionStatus)
    val fill: Color = if (contributor) colors.contributor else colors.dependant
    val label = member.displayLabel?.takeIf { it.isNotBlank() } ?: relationshipShort(member.relationship)
    Box(
        modifier = modifier.clip(CircleShape).background(fill.copy(alpha = if (focused) 0.95f else 0.8f))
            .then(if (selected) Modifier.border(3.dp, colors.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(label); append(", ")
                    append(if (contributor) "income contributor" else "dependant")
                    if (focused) append(", focused")
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label.take(12), style = FrrTypography.labelLarge, color = colors.elevatedSurface,
            textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseholdMemberEditor(
    member: HouseholdMember?,
    focusedId: String?,
    onUpdate: (HouseholdMember) -> Unit,
    onRemove: (String) -> Unit,
    onSetFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = frrColors()
    if (member == null) {
        Text(text = stringResource(R.string.household_select_prompt), style = FrrTypography.bodyLarge,
            color = colors.mutedText, modifier = modifier)
        return
    }
    var labelField by remember(member.id) { mutableStateOf(TextFieldValue(member.displayLabel.orEmpty())) }
    var ageField by remember(member.id) { mutableStateOf(TextFieldValue(member.age?.toString().orEmpty())) }
    var relationship by remember(member.id) { mutableStateOf(member.relationship) }
    var contribution by remember(member.id) { mutableStateOf(member.contributionStatus) }
    var dependency by remember(member.id) { mutableStateOf(member.dependencyStatus) }
    var ageError by remember { mutableStateOf<String?>(null) }

    fun persist(
        nextLabel: String? = labelField.text.trim().ifBlank { null },
        nextAge: Int? = ageField.text.trim().toIntOrNull(),
        nextRelationship: FamilyMemberType = relationship,
        nextContribution: ContributionStatus = contribution,
        nextDependency: DependencyStatus = dependency,
    ) {
        onUpdate(member.copy(
            displayLabel = nextLabel, age = nextAge, relationship = nextRelationship,
            contributionStatus = nextContribution, dependencyStatus = nextDependency,
        ))
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FrrSpacing.sm)) {
        Text(text = stringResource(R.string.household_editor_title), style = FrrTypography.titleLarge, color = colors.onSurface)
        FrrStableTextField(labelField, { labelField = it }, label = stringResource(R.string.household_label),
            supportingText = stringResource(R.string.household_label_hint), onFocusLost = { persist() })
        FrrStableTextField(
            ageField, { ageField = it; ageError = null },
            label = stringResource(R.string.household_age),
            isError = ageError != null, supportingText = ageError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onFocusLost = {
                val raw = ageField.text.trim()
                if (raw.isEmpty()) { ageError = null; persist(nextAge = null) }
                else {
                    val parsed = raw.toIntOrNull()
                    if (parsed == null || parsed !in HouseholdRules.MIN_AGE..HouseholdRules.MAX_AGE) {
                        ageError = "Age must be between ${HouseholdRules.MIN_AGE} and ${HouseholdRules.MAX_AGE}"
                    } else { ageError = null; persist(nextAge = parsed) }
                }
            },
        )
        EnumDropdown(stringResource(R.string.household_relationship), relationship, FamilyMemberType.entries, { relationshipLabel(it) }) {
            relationship = it; persist(nextRelationship = it)
        }
        EnumDropdown(stringResource(R.string.household_contribution), contribution, ContributionStatus.entries, { contributionLabel(it) }) {
            contribution = it; persist(nextContribution = it)
        }
        EnumDropdown(stringResource(R.string.household_dependency), dependency, DependencyStatus.entries, { dependencyLabel(it) }) {
            dependency = it; persist(nextDependency = it)
        }
        if (member.id != focusedId && isIncomeContributor(contribution)) {
            FrrSecondaryButton(
                text = stringResource(R.string.household_set_focus),
                onClick = { onSetFocus(member.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (member.id == focusedId) {
            Text(text = stringResource(R.string.household_is_focus), style = FrrTypography.bodyMedium, color = colors.primaryAction)
        }
        FrrTextAction(
            text = stringResource(R.string.household_remove),
            onClick = { onRemove(member.id) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable private fun relationshipLabel(type: FamilyMemberType) = when (type) {
    FamilyMemberType.SELF -> stringResource(R.string.relationship_self)
    FamilyMemberType.SPOUSE_OR_PARTNER -> stringResource(R.string.relationship_spouse)
    FamilyMemberType.CHILD -> stringResource(R.string.relationship_child)
    FamilyMemberType.PARENT -> stringResource(R.string.relationship_parent)
    FamilyMemberType.OTHER_DEPENDANT -> stringResource(R.string.relationship_other)
}

@Composable private fun relationshipShort(type: FamilyMemberType) = when (type) {
    FamilyMemberType.SELF -> stringResource(R.string.relationship_self_short)
    FamilyMemberType.SPOUSE_OR_PARTNER -> stringResource(R.string.relationship_spouse_short)
    FamilyMemberType.CHILD -> stringResource(R.string.relationship_child_short)
    FamilyMemberType.PARENT -> stringResource(R.string.relationship_parent_short)
    FamilyMemberType.OTHER_DEPENDANT -> stringResource(R.string.relationship_other_short)
}

@Composable private fun contributionLabel(status: ContributionStatus) = when (status) {
    ContributionStatus.PRIMARY_INCOME -> stringResource(R.string.contribution_primary)
    ContributionStatus.SHARED_INCOME -> stringResource(R.string.contribution_shared)
    ContributionStatus.SUPPLEMENTARY_OR_IRREGULAR -> stringResource(R.string.contribution_supplementary)
    ContributionStatus.ESSENTIAL_UNPAID_CAREGIVER -> stringResource(R.string.contribution_caregiver)
    ContributionStatus.NON_CONTRIBUTOR -> stringResource(R.string.contribution_none)
}

@Composable private fun dependencyLabel(status: DependencyStatus) = when (status) {
    DependencyStatus.FULLY_DEPENDENT -> stringResource(R.string.dependency_full)
    DependencyStatus.PARTLY_DEPENDENT -> stringResource(R.string.dependency_part)
    DependencyStatus.FINANCIALLY_INDEPENDENT -> stringResource(R.string.dependency_independent)
    DependencyStatus.NOT_APPLICABLE -> stringResource(R.string.dependency_na)
}
