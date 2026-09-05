package top.fatweb.apimanagement.controller.system

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import top.fatweb.apimanagement.annotation.BaseController
import top.fatweb.apimanagement.annotation.ProcessParam
import top.fatweb.apimanagement.entity.common.ResponseResult
import top.fatweb.apimanagement.exception.NoRecordFoundException
import top.fatweb.apimanagement.param.system.apiReport.ApiReportGetParam
import top.fatweb.apimanagement.service.api.IApiReportService
import top.fatweb.apimanagement.service.system.IStorageBlobService
import top.fatweb.apimanagement.vo.api.ApiReportVo
import top.fatweb.apimanagement.vo.api.ApiTopVo

/**
 * API report controller
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 * @see IApiReportService
 * @see IStorageBlobService
 */
@BaseController(path = ["/system/api/report"], name = "API 报表", description = "API 报表相关接口")
class ApiReportController(
    private val apiReportService: IApiReportService,
    private val storageBlobService: IStorageBlobService
) {
    /**
     * Get API usage report
     *
     * @param apiReportGetParam Get API report parameters
     * @return Response object includes report information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ResponseResult
     * @see ApiReportVo
     */
    @Operation(summary = "获取 API 用量报表")
    @GetMapping("/usage")
    @PreAuthorize("hasAnyAuthority('system:operations:report:usage')")
    fun usage(@ProcessParam @Valid apiReportGetParam: ApiReportGetParam?): ResponseResult<List<ApiReportVo>> =
        ResponseResult.databaseSuccess(data = apiReportService.usage(apiReportGetParam))

    /**
     * Get API cost report
     *
     * @param apiReportGetParam Get API report parameters
     * @return Response object includes report information
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ResponseResult
     * @see ApiReportVo
     */
    @Operation(summary = "获取 API 费用报表")
    @GetMapping("/cost")
    @PreAuthorize("hasAnyAuthority('system:operations:report:cost')")
    fun cost(@ProcessParam @Valid apiReportGetParam: ApiReportGetParam?): ResponseResult<List<ApiReportVo>> =
        ResponseResult.databaseSuccess(data = apiReportService.cost(apiReportGetParam))

    /**
     * Get API top list
     *
     * @param apiReportGetParam Get API report parameters
     * @return Response object includes top list
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ResponseResult
     * @see ApiTopVo
     */
    @Operation(summary = "获取 API Top 列表")
    @GetMapping("/top")
    @PreAuthorize("hasAnyAuthority('system:operations:report:top')")
    fun top(@ProcessParam @Valid apiReportGetParam: ApiReportGetParam?): ResponseResult<List<ApiTopVo>> =
        ResponseResult.databaseSuccess(data = apiReportService.top(apiReportGetParam))

    /**
     * Export API usage report as CSV, returns file hash
     *
     * @param apiReportGetParam Get API report parameters
     * @return Response object includes file hash
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     * @see ApiReportGetParam
     * @see ResponseResult
     */
    @Operation(summary = "导出 API 用量报表")
    @GetMapping("/export")
    @PreAuthorize("hasAnyAuthority('system:operations:report:export')")
    fun export(@ProcessParam @Valid apiReportGetParam: ApiReportGetParam?): ResponseResult<String> =
        ResponseResult.databaseSuccess(data = apiReportService.export(apiReportGetParam))

    /**
     * Download exported report file by file hash
     *
     * @param fileHash File hash
     * @return Report file bytes
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Operation(summary = "下载导出报表")
    @GetMapping("/export/{fileHash}")
    @PreAuthorize("hasAnyAuthority('system:operations:report:export')")
    fun download(@PathVariable fileHash: String): ResponseEntity<ByteArray> {
        val bytes = storageBlobService.loadFile(fileHash) ?: throw NoRecordFoundException()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=api-report.csv")
            .contentType(MediaType.TEXT_PLAIN)
            .body(bytes)
    }
}
