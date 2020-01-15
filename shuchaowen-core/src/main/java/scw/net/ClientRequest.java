package scw.net;

import java.io.IOException;

import scw.net.message.OutputMessage;

public interface ClientRequest extends OutputMessage, Request {
	ClientResponse execute() throws IOException;
}