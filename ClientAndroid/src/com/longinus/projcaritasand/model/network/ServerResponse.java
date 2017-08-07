package com.longinus.projcaritasand.model.network;

public class ServerResponse {
	private int code;
	private String content;
	private Exception exception;
	
	public ServerResponse(int code) {
		super();
		this.code = code;
	}
	
	public ServerResponse(int code, String content) {
		super();
		this.code = code;
		this.content = content;
	}
	
	public ServerResponse(Exception exception) {
		super();
		this.exception = exception;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	public boolean	hasException() {
		return exception!=null;
	}

	@Override
	public String toString() {
		return "ServerResponse [code=" + code + ", content=" + content + ", exception=" + exception + "]";
	}
}
