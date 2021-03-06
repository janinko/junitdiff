
package org.jboss.qa.junitdiff.export;

import cz.dynawest.xslt.XsltTransformer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.jboss.qa.junitdiff.model.AggregatedTestResults;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringEscapeUtils;
import org.jboss.qa.junitdiff.JUnitDiffApp;
import org.jboss.qa.junitdiff.ex.JUnitDiffException;
import org.jboss.qa.junitdiff.model.AggregatedData;
import org.jboss.qa.junitdiff.model.IGroup;
import org.jboss.qa.junitdiff.model.TestCaseInfo;
import org.jboss.qa.junitdiff.model.TestRunInfo;
import org.jboss.qa.junitdiff.model.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Ondrej Zizka
 */
public class XmlExporter
{
	private static final Logger log = LoggerFactory.getLogger(JUnitDiffApp.class);

    private static final String XSL_TEMPLATE_PATH = "/JUnitDiff-to-HTML.xsl";

    
	/**
	 *  Exports given matrix to the given file, as a JUnit-like XML.
	 */
	public static void exportToHtmlFile( AggregatedData aggData, File fout, String title ) throws JUnitDiffException
    {
        //exportToXML( atr, new PrintStream( fout, "uft8" ) );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportToXML( aggData, new PrintStream( baos ) );
        
        try {
          ReaderInputStream ris = new ReaderInputStream( new StringReader(baos.toString("utf8")), "utf8");
          InputStream xslTemplate = XmlExporter.class.getResourceAsStream( XSL_TEMPLATE_PATH );
          Map<String, Object> params = new HashMap();
          if( title != null ) params.put( "title", title );
          XsltTransformer.transform( ris, xslTemplate, fout, params );
        }
        catch( TransformerException ex ){
          throw new JUnitDiffException("Error when creating HTML file: "+ex.getMessage(), ex);
        }
        catch( UnsupportedEncodingException ex ) {
          throw new RuntimeException( ex );
        }
	}

    

	/**
	 *  Exports given matrix to the given file, as a JUnit-like XML.
	 */
	public static void exportToXML( AggregatedData aggData, File fout ) throws FileNotFoundException {

			exportToXML( aggData, new PrintStream(fout) );

	}


	/**
	 *  Exports given matrix to the given printstream, as a JUnit-like XML.
	 *  TODO: SAX-like output?
	 */
	public static void exportToXML( AggregatedData aggData, PrintStream out ) {
		out.println("<aggregate>");

		AggregatedTestResults atr = aggData.getAggregatedTestResults();
		// TODO: Move groups to AggregatedData? But that would be redundant... we have it in atr's map.


		// Groups.
		out.println("\t<groups>");
		atr.shortenGroupsNames();
		List<IGroup> groups = atr.getGroups();

		/*for (String group : groups) {
				out.append("\t\t<group name=\"").append(x( group )).append("\" path=\"").append(x( group )).append("\"/>\n");
		}*/

		for(IGroup g : groups){
			out.append("\t\t<group name=\"").append(x( g.getName() ))
					   .append("\" path=\"").append(x( g.getPath() ))
					   .append("\" id=\"").append(x( g.getId().toString() ))
					   .append("\"/>\n");
		}

		out.println("\t</groups>\n");



		// Test cases.
		for( TestCaseInfo testcase : atr.getTestCases() ) {
			out.append("\t<testcase classname=\"").append(x( testcase.getClassName() ))
			   .append("\" name=\"").append(x( testcase.getName() )) .append("\">\n");

			for( TestRunInfo testrun : testcase.getTestRuns() ){
				out.append("\t\t<testrun result=\"").append(x( testrun.getResult().name() ))
				   .append("\" time=\"").append(x( testrun.getTime() ))
				   .append("\" group=\"").append(x( testrun.getGroupID() ))
				   .append("\">\n");

				// <failure message="Exception message" type="java.lang.Exception">
				if( null != testrun.getFailure() ){
					out.append("\t\t\t<failure message=\"").append(x( testrun.getFailure().getMessage() ))
					   .append("\" type=\"").append(x( testrun.getFailure().getType() )).append("\">\n");
					out.print(x( testrun.getFailure().getTrace() ));
					out.println("</failure>");
				}

				out.println("\t\t</testrun>");
			}
			out.println("\t</testcase>");
		}


		// Testsuites,  BTW TODO:  Rename the top-level element.
		// TODO: Add TestSuite reference to TestInfo and delegate TestInfo.getOrigin() to that.
		// TODO: Perhaps the test cases could be moved to the TestSuite, after all.

		out.println("\t<testsuites>");
		for( TestSuite ts : aggData.getTestSuites() ){
			out.append("\t\t<testsuite group=\"").append(x( ts.getGroup() ))
				 .append("\" name=\"").append(x( ts.getClassName() ))
				 .append("\" origin=\"").append(x( ts.getOrigin() ))
				 .append("\">\n");

			out.append("\t\t<system-out><![CDATA[").append( ts.getStdOut() ).append("]]></system-out>\n");
			out.append("\t\t<system-err><![CDATA[").append( ts.getStdErr() ).append("]]></system-err>\n");

							out.println("\t\t</testsuite>");
			}
			out.println("\t</testsuites>\n");


			out.println("</aggregate>");
		}


	/** Helper - XML escape. */
	private static String x( String s ){ return StringEscapeUtils.escapeXml( s ); }


}// class
