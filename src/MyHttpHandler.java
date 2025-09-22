import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import java.util.List;

// 修改构造函数以接收HeaderTableModel
public class MyHttpHandler implements HttpHandler {
    private final CustomLogger.HeaderTableModel headerTableModel;

    // 构造函数接收请求头表格模型
    public MyHttpHandler(CustomLogger.HeaderTableModel headerTableModel) {
        this.headerTableModel = headerTableModel;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        // 从表格模型获取所有用户添加的请求头
        List<CustomLogger.HeaderEntry> customHeaders = headerTableModel.getAllHeaders();

        // 从原始请求创建一个可修改的副本
        HttpRequest modifiedRequest = requestToBeSent;

        // 遍历所有自定义请求头并添加到请求中
        for (CustomLogger.HeaderEntry header : customHeaders) {
            // 先移除可能存在的同名请求头，避免冲突
            modifiedRequest = modifiedRequest.withRemovedHeader(header.name);
            // 添加用户定义的请求头
            modifiedRequest = modifiedRequest.withAddedHeader(header.name, header.value);
        }

        // 使用修改后的请求继续
        return RequestToBeSentAction.continueWith(modifiedRequest);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        // 如果需要记录响应，可以在这里处理
        return ResponseReceivedAction.continueWith(responseReceived);
    }
}
