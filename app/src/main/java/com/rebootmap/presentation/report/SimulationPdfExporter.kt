package com.rebootmap.presentation.report

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.rebootmap.domain.model.CashFlowProjection
import com.rebootmap.presentation.components.formatKoreanMan
import com.rebootmap.presentation.simulation.SimulationUiState
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
            putExtra(Intent.EXTRA_SUBJECT, "Reboot Map 노후 시뮬레이션 리포트")
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

        canvas.drawText("Reboot Map 노후 시뮬레이션 리포트", MARGIN, y, titlePaint)
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

        lines += "## 기본 정보"
        lines += "현재 ${profile.currentAge}세 · 목표 은퇴 ${profile.retirementAge}세"
        lines += "목표 월 생활비: ${formatKoreanMan(profile.monthlyLivingExpense)}"
        if (profile.lifeExpectancy > profile.currentAge) {
            lines += "기대 수명: ${profile.lifeExpectancy}세"
        }
        lines += "물가상승률: ${"%.1f".format(state.assumptions.inflationRate * 100)}%"

        projection?.let { appendProjection(lines, it, profile.retirementAge) }

        if (state.lumpSumExpenses.isNotEmpty()) {
            lines += "## 목돈 지출 타임라인"
            state.lumpSumExpenses.sortedBy { it.year }.forEach { expense ->
                lines += "${expense.year}년 · ${expense.displayLabel()} · ${formatKoreanMan(expense.amount)}"
                state.expenseMatches[expense.id].orEmpty().forEach { suggestion ->
                    lines += "  → ${suggestion.assetLabel}: ${formatKoreanMan(suggestion.availableAmount)} (${suggestion.timingNote})"
                }
            }
        }

        if (state.relocationPlan.isConfigured()) {
            lines += "## 거주지 이동 시나리오"
            lines += "신규 주택 순자산: ${formatKoreanMan(state.relocationPlan.newHomeEquity)}"
        }

        lines += "## 권고 가이드"
        lines += guidelineText(projection)

        return lines
    }

    private fun appendProjection(
        lines: MutableList<String>,
        projection: CashFlowProjection,
        retirementAge: Int,
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
        val assetDecline = projection.assetDeclineYears(retirementAge)
        if (assetDecline.isNotEmpty()) {
            lines += "실제 자산 감소: ${projection.yearSpanSummary(assetDecline).headline}"
        }
        if (projection.deficitYears.isNotEmpty()) {
            lines += "수입 부족 연도: ${projection.yearSpanSummary(projection.deficitYears).headline}"
        }
    }

    private fun guidelineText(projection: CashFlowProjection?): String = when {
        projection == null -> "온보딩 및 자산 입력을 완료한 뒤 리포트를 생성하세요."
        projection.depletionYear == null ->
            "현재 입력 기준으로 기대 수명까지 자산 유지가 가능합니다. 목돈 지출·물가상승률 변동에 따른 재점검을 권장합니다."
        projection.finalBalance < 0 ->
            "자산 고갈 이후 적자가 누적될 수 있습니다. 연금 수령 시점 조정, 부동산 유동화, 생활비 절감 시나리오를 검토하세요."
        else ->
            "은퇴 이후 일정 시점에 자산 고갈이 예상됩니다. 목돈 지출 시점과 가용 자산(적금 만기·매각 등) 매칭을 확인하세요."
    }
}
