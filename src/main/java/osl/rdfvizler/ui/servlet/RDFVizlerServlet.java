package osl.rdfvizler.ui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.rdf.model.Model;

import osl.rdfvizler.dot.DotModel;
import osl.rdfvizler.dot.DotProcess;
import osl.rdfvizler.dot.RDF2Dot;
import osl.util.rdf.Models;

public class RDFVizlerServlet extends HttpServlet {

	private static final long serialVersionUID = 7193847752589093476L;

	// URL params
	private final static String
	pRDF = "rdf",
	pRules = "rules",
	pRDFFormat = "in",
	pDotFormat = "out";

	// settings available in web.xml
	private String MaxFileSize = "500000";
	private String DotExec = "/usr/bin/dot";
	private String DefaultRule = null;

	private final String UTF8 = "UTF-8";


	public <T> T getValue (T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		DotExec     = getValue(config.getInitParameter("DotExec"), DotExec);
		MaxFileSize = getValue(config.getInitParameter("MaxInput"), MaxFileSize);
		// TODO DefaultRule = getValue(config.getInitParameter("DefaultRule"), DefaultRule);
	}

	@Override
	protected void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException {

		// default values
		String 
		pathRDF = null, 
		pathRules = DefaultRule, 
		formatRDF = "TTL",
		formatDot = "svg";

		try {
			pathRDF   = getValue(request.getParameter(pRDF), pathRDF);
			pathRules = getValue(request.getParameter(pRules), pathRules);
			formatRDF = getValue(request.getParameter(pRDFFormat), formatRDF);
			formatDot = getValue(request.getParameter(pDotFormat), formatDot);
			
			int maxSize = Integer.parseInt(MaxFileSize);
			
			DotModel.checkURIInput(pathRDF, maxSize);
			DotModel.checkURIInput(pathRules, maxSize);

			Model model = DotModel.getDotModel(pathRDF, formatRDF, pathRules);
			String dot = RDF2Dot.toDot(model);

			DotProcess dotProcess = new DotProcess(DotExec);
			String out;
			String mimetype;

			if (formatDot.equals("svg")) {
				out = dotProcess.runDot(dot, formatDot);
				mimetype = "image/svg+xml";
			} 
			/* TODO does not work, needs to output as image
			else if (formatDot.equals("png")) {
				out = dotProcess.runDot(dot, formatDot);
				mimetype = "image/png";
			}*/
			 else if (formatDot.equals("ttl")) {
				out = Models.writeModel(model, "TTL");
				mimetype = "text/turtle";
			} else {
				out = dot;
				mimetype = "text/plain";
			}
			respond (response, out, mimetype);
		} catch (RuntimeException | IOException e) {
			printError(request, response, 500, pathRDF, pathRules, e); 
		}
	}

	// pass content on to response's writer
	private void respond (HttpServletResponse response, String content, String mimetype) throws IOException {
		response.setCharacterEncoding(UTF8);
		response.setContentType(mimetype);
		PrintWriter writer = response.getWriter();
		writer.write(content);
		writer.flush();
		writer.close();
	}

	// nice-ish error page
	private void printError (HttpServletRequest request, HttpServletResponse response, int responseCode,
			String pathRDF, String pathRules, Exception e) throws IOException {

		response.setStatus(responseCode);
		response.setContentType("text/html;charset=\"UTF-8\"");

		PrintWriter writer = response.getWriter();
		String error = "<html><head><meta http-equiv=\"Content-type\" content=\"text/html;charset=UTF-8\" />";
		error += "<title>RDFVizler - Error " + responseCode + "</title>";
		error += "</head><body>";
		error += "<h1>RDFVizler - Error " + responseCode + "</h1>";
		error += "<dl>";
		error += "<dt>Service:</dt><dd>" + request.getRequestURL() + "</dd>";
		//error += "<dt>Parameters:</dt><dd>" + Arrays.toString(request.getParameterMap().entrySet().toArray()) + "</dd>";
		error += "<dt>RDF:</dt><dd> " + pathRDF + "</dd>";
		error += "<dt>Rules:</dt><dd> " + pathRules + "</dd>";
		error += "<dt>Error message:</dt><dd><code>" + StringEscapeUtils.escapeHtml4(e.getMessage()) + "</code></dd>";
		error += "<dt>Error stack:</dt><dd><pre>" + StringEscapeUtils.escapeHtml4(Arrays.toString(e.getStackTrace())).replaceAll(",", "<br/>") + "</pre></dd>";
		error += "</dl>";
		error += "</body></html>";
		writer.print(error);
		writer.flush();
		writer.close();
	}
}