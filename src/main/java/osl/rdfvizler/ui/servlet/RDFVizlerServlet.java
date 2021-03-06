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
	private static final String
	pRDF = "rdf",
	pRules = "rules",
	pRDFFormat = "in",
	pDotFormat = "out";

	// defaults, also available in web.xml
	private String defaultMaxFileSize = "300000";
	private String defaultDotExec = "/usr/bin/dot";
	private String defaultPathRules = "https://mgskjaeveland.github.io/rdfvizler/rules/rdf.jrule";
	private String defaultFormatRDF = "TTL";
	private String defaultFormatDot = "svg";
	
	private String MaxFileSize;
	private String DotExec;

	private final String UTF8 = "UTF-8";

	private static <T> T getValue (T value, T defaultValue) {
		return value != null ? value : defaultValue;
	}

	// possibly overwrite defaults with values from web.xml
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		DotExec           = getValue(config.getInitParameter("DotExec"), defaultDotExec);
		MaxFileSize       = getValue(config.getInitParameter("MaxInput"), defaultMaxFileSize);
		
		defaultPathRules  = getValue(config.getInitParameter("DefaultRules"), defaultPathRules);
		defaultFormatRDF  = getValue(config.getInitParameter("DefaultFormatRDF"), defaultFormatRDF);
		defaultFormatDot  = getValue(config.getInitParameter("DefaultFormatDot"), defaultFormatDot);
	}

	@Override
	protected void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException {

		String pathRDF = null, 
				pathRules = null, 
				formatRDF, 
				formatDot;

		try {
			pathRDF   = getValue(request.getParameter(pRDF), pathRDF);
			pathRules = getValue(request.getParameter(pRules), defaultPathRules);
			formatRDF = getValue(request.getParameter(pRDFFormat), defaultFormatRDF);
			formatDot = getValue(request.getParameter(pDotFormat), defaultFormatDot);

			int maxSize = Integer.parseInt(MaxFileSize);

			DotModel.checkURIInput(pathRDF, maxSize);
			DotModel.checkURIInput(pathRules, maxSize);

			Model model = DotModel.getDotModel(pathRDF, formatRDF, pathRules);
			String dot = RDF2Dot.toDot(model);

			DotProcess dotProcess = new DotProcess(DotExec);
			String out;
			String mimetype;

			if ("svg".equals(formatDot)) {
				out = dotProcess.runDot(dot, formatDot);
				mimetype = "image/svg+xml";
			} 
			/* TODO does not work, needs to output as image
			else if ("png".equals(formatDot)) {
				out = dotProcess.runDot(dot, formatDot);
				mimetype = "image/png";
			}*/
			else if ("ttl".equals(formatDot)) {
				out = Models.writeModel(model, "TTL");
				mimetype = "text/turtle";
			} else {
				out = dot;
				mimetype = "text/plain";
			}
			respond (response, out, mimetype);
		} catch (RuntimeException | IOException e) {
			respond (response, getErrorMessage(500, pathRDF, pathRules, e), "text/html"); 
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

	private String getErrorMessage (int responseCode, String pathRDF, String pathRules, Exception e) {
		String error = "<html><head><meta http-equiv=\"Content-type\" content=\"text/html;charset=UTF-8\" />";
		error += "<title>RDFVizler - Error " + responseCode + "</title>";
		error += "</head><body>";
		error += "<h1>RDFVizler - Error " + responseCode + "</h1>";
		error += "<dl>";
		//error += "<dt>Service:</dt><dd>" + request.getRequestURL() + "</dd>";
		//error += "<dt>Parameters:</dt><dd>" + Arrays.toString(request.getParameterMap().entrySet().toArray()) + "</dd>";
		error += "<dt>RDF:</dt><dd> " + pathRDF + "</dd>";
		error += "<dt>Rules:</dt><dd> " + pathRules + "</dd>";
		error += "<dt>Error message:</dt><dd><code>" + StringEscapeUtils.escapeHtml4(e.getMessage()) + "</code></dd>";
		error += "<dt>Error stack:</dt><dd><pre>" + StringEscapeUtils.escapeHtml4(Arrays.toString(e.getStackTrace())).replaceAll(",", "<br/>") + "</pre></dd>";
		error += "</dl>";
		error += "</body></html>";
		return error;
	}
}
