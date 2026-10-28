package top.fatweb.apimanagement.api.v1

/**
 * Handles one API request.
 *
 * @author FatttSnake, fatttsnake@gmail.com
 * @since 1.0.0
 */
fun interface ApiHandler {
    /**
     * Handles a request.
     *
     * @param request request snapshot
     * @return response
     * @throws Exception when the plugin cannot produce a response
     * @author FatttSnake, fatttsnake@gmail.com
     * @since 1.0.0
     */
    @Throws(Exception::class)
    fun handle(request: ApiRequest): ApiResponse
}
