package com.rebootmap.presentation.report



import android.content.Context

import android.content.Intent

import android.graphics.Paint

import android.graphics.pdf.PdfDocument

import androidx.core.content.FileProvider

import com.rebootmap.domain.advisory.AssetAdvisoryEngine

import com.rebootmap.domain.advisory.CashFlowHighlightPlanner

import com.rebootmap.domain.model.Asset

import com.rebootmap.domain.model.CashFlowProjection

import com.rebootmap.domain.model.YearSnapshot

import com.rebootmap.domain.tax.AnnualTaxBreakdown

import com.rebootmap.presentation.components.formatKoreanMan

import com.rebootmap.presentation.simulation.CashFlowTableFormat

import com.rebootmap.presentation.simulation.SimulationUiState

import com.rebootmap.presentation.simulation.displayTitle

import com.rebootmap.domain.portfolio.RealEstateTimingAdvisoryEngine

import com.rebootmap.presentation.simulation.summaryText

import java.io.File

import java.io.FileOutputStream

import java.time.LocalDate

import java.time.format.DateTimeFormatter



object SimulationPdfExporter {



    private const val PAGE_WIDTH = 595

    private const val PAGE_HEIGHT = 842

    private const val MARGIN = 40f

    private const val LINE_HEIGHT = 18f



    fun exportAndShare(context: Context, state: SimulationUiState) {

        val file = buildPdf(context, state)

        val uri = FileProvider.getUriForFile(

            context,

            "${context.packageName}.fileprovider",

            file,

        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {

            type = "application/pdf"

            putExtra(Intent.EXTRA_STREAM, uri)

            putExtra(Intent.EXTRA_SUBJECT, "Reboot Map 자산운용 리포트")

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        }

        context.startActivity(Intent.createChooser(shareIntent, "리포트 공유"))

    }



    fun buildPdf(context: Context, state: SimulationUiState): File {

        val lines = buildReportLines(state)

        val document = PdfDocument()

        val titlePaint = Paint().apply {

            textSize = 16f

            isFakeBoldText = true

        }

        val bodyPaint = Paint().apply { textSize = 11f }

        val sectionPaint = Paint().apply {

            textSize = 13f

            isFakeBoldText = true

        }



        var pageNumber = 1

        var page = document.startPage(

            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),

        )

        var canvas = page.canvas

        var y = MARGIN + 20f



        fun newPageIfNeeded(required: Float = LINE_HEIGHT) {

            if (y + required > PAGE_HEIGHT - MARGIN) {

                document.finishPage(page)

                pageNumber++

                page = document.startPage(

                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),

                )

                canvas = page.canvas

                y = MARGIN

            }

        }



        canvas.drawText("Reboot Map 자산운용 리포트", MARGIN, y, titlePaint)

        y += LINE_HEIGHT * 1.5f

        canvas.drawText(

            "생성일: ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}",

            MARGIN,

            y,

            bodyPaint,

        )

        y += LINE_HEIGHT * 2



        lines.forEach { line ->

            val paint = when {

                line.startsWith("## ") -> sectionPaint.also { y += LINE_HEIGHT * 0.5f }

                else -> bodyPaint

            }

            val text = line.removePrefix("## ")

            newPageIfNeeded()

            canvas.drawText(text, MARGIN, y, paint)

            y += if (line.startsWith("## ")) LINE_HEIGHT * 1.4f else LINE_HEIGHT

        }



        document.finishPage(page)



        val file = File(context.cacheDir, "reboot_map_report_${System.currentTimeMillis()}.pdf")

        FileOutputStream(file).use { document.writeTo(it) }

        document.close()

        return file

    }



    private fun buildReportLines(state: SimulationUiState): List<String> {

        val profile = state.profile

        val projection = state.projection

        val lines = mutableListOf<String>()



        val advisory = AssetAdvisoryEngine.evaluate(

            projection,

            profile,

            state.assets,

            state.personalLoans,

        )

        lines += "## 자산운용 총평"

        lines += "종합 ${advisory.score}점 (${advisory.gradeLabel}) — ${advisory.headline}"

        lines += advisory.summary

        advisory.strengths.forEach { lines += "  [잘된 점] $it" }

        advisory.weaknesses.forEach { lines += "  [부족한 점] $it" }

        advisory.watchPoints.forEach { lines += "  [유의할 점] $it" }



        lines += "## 기본 정보"

        lines += "현재 ${profile.currentAge}세 · 목표 은퇴 ${profile.retirementAge}세"

        lines += "목표 월 생활비: ${formatKoreanMan(profile.monthlyLivingExpense)}"

        if (profile.lifeExpectancy > profile.currentAge) {

            lines += "기대 수명: ${profile.lifeExpectancy}세"

        }

        lines += "물가상승률: ${"%.1f".format(state.assumptions.inflationRate * 100)}%"

        val expenseBase = when (state.assumptions.livingExpenseInflationBase) {

            com.rebootmap.domain.model.LivingExpenseInflationBase.RETIREMENT_AGE -> "은퇴 시점 기준"

            com.rebootmap.domain.model.LivingExpenseInflationBase.SIMULATION_START -> "현재부터 누적"

        }

        lines += "생활비 물가 기준: $expenseBase"

        lines += "국민연금: 물가연동 · 퇴직·개인연금: 운용수익률 · 노랑우산: 공제이자 일시금"

        val assumptions = state.assumptions

        lines += "세금·보험 반영: 재산세 ${if (assumptions.propertyTaxEnabled) "ON" else "OFF"}, " +

            "종부세 ${if (assumptions.comprehensiveRealEstateTaxEnabled) "ON" else "OFF"}, " +

            "건보 ${if (assumptions.healthInsuranceEnabled) "ON" else "OFF"} (간이 추정)"



        appendAssetSummary(lines, state.assets)



        projection?.let { appendProjection(lines, it, profile, state.assets) }



        if (state.lumpSumExpenses.isNotEmpty()) {

            lines += "## 목돈 지출 타임라인"

            state.lumpSumExpenses.sortedBy { it.year }.forEach { expense ->

                lines += "${expense.year}년 · ${expense.displayLabel()} · ${formatKoreanMan(expense.amount)}"

                state.expenseMatches[expense.id].orEmpty().forEach { suggestion ->

                    lines += "  → ${suggestion.assetLabel}: ${formatKoreanMan(suggestion.availableAmount)} (${suggestion.timingNote})"

                }

            }

        }



        val activeLoans = state.personalLoans.filter { it.isSimulationReady() }

        if (activeLoans.isNotEmpty()) {

            lines += "## 신용·차용 부채"

            activeLoans.forEachIndexed { index, loan ->

                val label = loan.displayTitle(index, activeLoans.size)

                lines += "$label: ${formatKoreanMan(loan.balance)} · 연 ${(loan.annualInterestRate * 100).toInt()}%"

                if (loan.monthlyPayment > 0) {

                    lines += "  월 상환 ${formatKoreanMan(loan.monthlyPayment)}"

                }

            }

        }



        val estates = state.assets.filterIsInstance<Asset.RealEstate>()

        if (estates.any { it.currentValue > 0 || it.debtAmount > 0 }) {

            val startYear = java.time.Year.now().value

            val timing = RealEstateTimingAdvisoryEngine.evaluate(estates, startYear)

            lines += "## 부동산 보유 타이밍 컨설팅"

            lines += timing.headline

            lines += timing.summary

            timing.strengths.forEach { lines += "  [잘된 점] $it" }

            timing.weaknesses.forEach { lines += "  [유의] $it" }

            timing.watchPoints.forEach { lines += "  [참고] $it" }

            if (timing.overlapYears.isNotEmpty()) {

                lines += "2주택 겹침 연도: ${timing.overlapYears.joinToString(", ")}"

            }

            val taxEst = RealEstateTimingAdvisoryEngine.estimateTransactionTaxWon(estates, startYear)

            if (taxEst > 0) {

                lines += "거래 관련 세금·중개료 합계(간이): ${formatKoreanMan(taxEst)}"

            }

        }



        lines += "## 안내"

        lines += "본 리포트는 입력값 기반 교육·계획 참고용이며 투자·세무 자문이 아닙니다."

        lines += "실제 세무·연금·부동산 결정은 전문가 상담을 권합니다."



        return lines

    }



    private fun appendAssetSummary(lines: MutableList<String>, assets: List<Asset>) {

        val configured = assets.filter { it.summaryText() != "미입력" }

        if (configured.isEmpty()) return



        lines += "## 입력 자산 요약"

        val estates = assets.filterIsInstance<Asset.RealEstate>()

        configured.forEach { asset ->

            val title = when (asset) {

                is Asset.RealEstate -> {

                    val index = estates.indexOfFirst { it.id == asset.id }.coerceAtLeast(0)

                    asset.displayTitle(estateOrdinal = index, estateCount = estates.size)

                }

                else -> asset.displayTitle()

            }

            lines += "$title: ${asset.summaryText()}"

        }

    }



    private fun appendProjection(

        lines: MutableList<String>,

        projection: CashFlowProjection,

        profile: com.rebootmap.domain.model.UserProfile,

        assets: List<Asset>,

    ) {

        lines += "## 시뮬레이션 결과"

        lines += if (projection.depletionYear == null) {

            "자산 유지 가능 (기대 수명까지)"

        } else {

            "자산 고갈 예상: ${projection.depletionYear}년"

        }

        lines += if (projection.finalBalance < 0) {

            "예상 부채(적자 누적): ${formatKoreanMan(projection.finalBalance)}"

        } else {

            "최종 예상 자산: ${formatKoreanMan(projection.finalBalance)}"

        }

        lines += "시뮬레이션 기간: ${projection.yearlySnapshots.size}년"

        val assetDecline = projection.assetDeclineYears(profile.retirementAge)

        if (assetDecline.isNotEmpty()) {

            lines += "실제 자산 감소: ${projection.yearSpanSummary(assetDecline).headline}"

        }

        if (projection.deficitYears.isNotEmpty()) {

            lines += "수입 부족 연도: ${projection.yearSpanSummary(projection.deficitYears).headline}"

        }



        appendHighlightCashFlow(lines, projection, profile, assets)

        appendPostRetirementTable(lines, projection, profile.retirementAge)

    }



    private fun appendHighlightCashFlow(

        lines: MutableList<String>,

        projection: CashFlowProjection,

        profile: com.rebootmap.domain.model.UserProfile,

        assets: List<Asset>,

    ) {

        val highlights = CashFlowHighlightPlanner.highlights(projection, profile, assets)

        if (highlights.isEmpty()) return



        lines += "## 전환 시점 현금흐름 요약"

        lines += "은퇴·연금·매각·고갈 등 주요 이벤트 연도 (정기 수입·세금 기준, 일시 유입 별도)"

        highlights.forEach { highlight ->

            val label = highlight.labels.joinToString(" · ")

            lines += "[${label}]"

            appendSnapshotCashFlow(lines, highlight.snapshot)

        }

    }



    private fun appendPostRetirementTable(

        lines: MutableList<String>,

        projection: CashFlowProjection,

        retirementAge: Int,

    ) {

        val postRetirement = CashFlowHighlightPlanner.postRetirementSnapshots(projection, retirementAge)

        if (postRetirement.isEmpty()) return



        lines += "## 은퇴 후 연도별 상세 (${postRetirement.size}년)"

        lines += "연도(나이) · 월 정기수입 · 월 생활비 · 월 세금 · 월 순현금 (일시 유입 있으면 별도 표기)"

        postRetirement.forEach { snapshot ->

            lines += CashFlowTableFormat.compactLine(snapshot)

            if (snapshot.taxBreakdown.hasAnyTax() || snapshot.annualHoldingCost.hasAnyHolding()) {

                appendTaxBreakdownLines(lines, snapshot.taxBreakdown, snapshot.annualHoldingCost)

            }

        }

    }



    private fun appendSnapshotCashFlow(lines: MutableList<String>, snapshot: YearSnapshot) {

        lines += CashFlowTableFormat.compactLine(snapshot)

        if (snapshot.incomeBreakdown.lumpSumTotal > 0) {

            lines += "  일시 유입: ${formatKoreanMan(snapshot.incomeBreakdown.lumpSumTotal)}/년"

        }

        appendTaxBreakdownLines(lines, snapshot.taxBreakdown, snapshot.annualHoldingCost)

    }



    private fun appendTaxBreakdownLines(

        lines: MutableList<String>,

        breakdown: AnnualTaxBreakdown,

        holding: com.rebootmap.domain.tax.AnnualHoldingCost,

    ) {

        val items = buildList {

            if (breakdown.employmentIncomeTax > 0) add("근로소득세" to breakdown.employmentIncomeTax)

            if (breakdown.businessIncomeTax > 0) add("사업소득세" to breakdown.businessIncomeTax)

            if (breakdown.pensionIncomeTax > 0) add("연금소득세" to breakdown.pensionIncomeTax)

            if (breakdown.otherIncomeTax > 0) add("기타소득세" to breakdown.otherIncomeTax)

            if (breakdown.capitalGainsTax > 0) add("양도소득세" to breakdown.capitalGainsTax)
            if (breakdown.acquisitionTax > 0) add("취득세" to breakdown.acquisitionTax)
            if (breakdown.brokerageFee > 0) add("중개보수" to breakdown.brokerageFee)

            if (breakdown.healthInsurance > 0) add("건강보험료" to breakdown.healthInsurance)

            if (breakdown.longTermCare > 0) add("장기요양보험" to breakdown.longTermCare)

            if (holding.residentialPropertyTax > 0) add("재산세(주거용)" to holding.residentialPropertyTax)

            if (holding.nonResidentialPropertyTax > 0) add("재산세(비주거용)" to holding.nonResidentialPropertyTax)

            if (holding.propertyTax > 0 &&

                holding.residentialPropertyTax == 0L &&

                holding.nonResidentialPropertyTax == 0L

            ) {

                add("재산세" to holding.propertyTax)

            }

            if (holding.comprehensiveRealEstateTax > 0) add("종부세" to holding.comprehensiveRealEstateTax)

        }

        items.forEach { (label, annual) ->

            lines += "  · $label: ${formatKoreanMan(annual)}/년 (${formatKoreanMan(annual / 12)}/월)"

        }

    }



    private fun AnnualTaxBreakdown.hasAnyTax(): Boolean =

        employmentIncomeTax > 0 || businessIncomeTax > 0 || pensionIncomeTax > 0 ||

            otherIncomeTax > 0 || capitalGainsTax > 0 || healthInsurance > 0 || longTermCare > 0



    private fun com.rebootmap.domain.tax.AnnualHoldingCost.hasAnyHolding(): Boolean =

        propertyTax > 0 || residentialPropertyTax > 0 || nonResidentialPropertyTax > 0 ||

            comprehensiveRealEstateTax > 0

}


