//------------------------------------------------------------------------------
// Copyright (C) 2011 by Free Java Code
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//------------------------------------------------------------------------------

package freejavacode.parser.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import freejavacode.parser.HtmlParser;
import freejavacode.parser.JavaCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Test harness for {@link HtmlParser} class.  A good example of how to
 * use the API.
 * 
 * @author Free Java Code
 *
 */
public class TestHtmlParser
{
    JavaCharsets jc = new JavaCharsets();
    
    /**
     * Sends file into Java's built-in XML parser.  This is the ultimate goal
     * of the {@link HtmlParser} class -- converting rogue HTML from the
     * Internet to use with Java's built-in XML parser (without getting any
     * errors).
     * 
     * @param file file to be parsed by Java's built-in XML parser
     * @return success or failure
     */
    public boolean verifyXmlParsing( String file )
    {
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.parse( file );
            System.out.println( "XML Parsing Successful for " + file );
            return( true );
        }
        catch( Exception ex )
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            ex.printStackTrace( pw );
            sw.toString();
            System.out.println( sw );
            System.out.println( "XML Parsing Unsuccessful for " + file );
            return( false );
        }
    }
    
    private HashMap< String, String > hmEntityNamesValues = new HashMap< String, String >();
    
    /**
     * Initializes a hash map of entity names/values for use in
     * "compareHtmlLanguage()" method.
     */
    public void initHtmlLanguageExceptions()
    {
        Set< String > s = HtmlParser.mEntityNames.keySet();
        java.util.Iterator< String > it = s.iterator();
        while( it.hasNext() )
        {
            String key = it.next();
            hmEntityNamesValues.put(  key, key );
        }
        Collection< String > c = HtmlParser.mEntityNames.values();
        it = c.iterator();
        while( it.hasNext() )
        {
            String value = it.next();
            hmEntityNamesValues.put(  value.substring( 1 ), value );
        }        
    }
    
    private class Token implements Comparable< Token >
    {
        public String word;
        public int pos;
        public int compareTo( Token t )
        {
            int ret = word.compareTo( t.word );
            if( ret == 0 )
                return( pos - t.pos );
            return( ret );
        }
    }
    
    /**
     * The method was created to help detect problems with the parser -
     * comparing the "HTML only" before and after.
     * 
     * @param encoding character set of the files
     * @param file1 the "before" file
     * @param file2 the "after" file
     */
    public void compareHtmlLanguage( String encoding, String file1, String file2 )
    {
        int issues = 0;
        System.out.println( "Comparing Html Language between...\nFile1(" + file1 + ") and...\nFile2(" + file2 + ")" );
        try
        {
            File f1 = new File( file1 );
            File f2 = new File( file2 );
            FileInputStream fis1 = new FileInputStream( f1 );
            FileInputStream fis2 = new FileInputStream( f2 );
            byte[] b1 = new byte[ ( int )f1.length() ];
            byte[] b2 = new byte[ ( int )f2.length() ];
            fis1.read( b1, 0, b1.length );
            fis2.read( b2, 0, b2.length );
            String s1 = new String( b1, encoding );
            String s2 = new String( b2, encoding );
            ArrayList< Token > al1 = new ArrayList< Token >();
            Token t1 = new Token();
            t1.word = "";
            t1.pos = -1;
            char[] ac1 = s1.toCharArray();
            int cpCount = Character.codePointCount( ac1, 0, ac1.length );
            int[] acp = new int[ 1 ]; 
            int offset = 0;
            for( int i = 0; i < cpCount; )
            {
                // Process codepoint
                int cp = Character.codePointAt( ac1, i );
                int chCount = Character.charCount( cp );
                acp[ 0 ] = cp;
                String scp = new String( acp, 0, 1 );
                int bCount = 0;
                try
                {
                    byte[] bytes = scp.getBytes( encoding );
                    bCount = bytes.length;
                }
                catch( Exception ex ) {}
                i += chCount;
                // Check codepoint
                if( Character.isLetterOrDigit( cp ) ||
                    cp == '_' )
                {
                    if( t1.pos == -1 || t1.pos == 0 )
                        t1.pos = offset;
                    t1.word += scp;
                    offset += bCount;
                }
                else
                {
                    offset += bCount;
                    if( t1.pos == -1 || t1.word.length() == 0 )
                    {
                        t1.pos = 0;
                        continue;
                    }
                    al1.add( t1 );
                    t1 = new Token();
                    t1.word = "";
                    t1.pos = 0;
                }
            }
            if( t1.word.length() > 0 )
                al1.add( t1 );
            ArrayList< Token > al2 = new ArrayList< Token >();
            Token t2 = new Token();
            t2.word = "";
            t2.pos = -1;
            char[] ac2 = s2.toCharArray();
            cpCount = Character.codePointCount( ac2, 0, ac2.length );
            offset = 0;
            for( int i = 0; i < cpCount; )
            {
                // Process codepoint
                int cp = Character.codePointAt( ac2, i );
                int chCount = Character.charCount( cp );
                acp[ 0 ] = cp;
                String scp = new String( acp, 0, 1 );
                int bCount = 0;
                try
                {
                    byte[] bytes = scp.getBytes( encoding );
                    bCount = bytes.length;
                }
                catch( Exception ex ) {}
                i += chCount;
                // Check codepoint
                if( Character.isLetterOrDigit( cp ) ||
                    cp == '_' )
                {
                    if( t2.pos == -1 || t2.pos == 0 )
                        t2.pos = offset;
                    t2.word += scp;
                    offset += bCount;
                }
                else
                {
                    offset += bCount;
                    if( t2.pos == -1 || t2.word.length() == 0 )
                    {
                        t2.pos = 0;
                        continue;
                    }
                    al2.add( t2 );
                    t2 = new Token();
                    t2.word = "";
                    t2.pos = 0;
                }
            }
            if( t2.word.length() > 0 )
                al2.add( t2 );
            Collections.sort( al1 );
            Collections.sort( al2 );
            java.util.Iterator< Token > it1 = al1.iterator();
            java.util.Iterator< Token > it2 = al2.iterator();
            t1 = it1.next();
            t2 = it2.next();
            int tc1 = 0;
            int tc2 = 0;
            Token pt1 = new Token();
            Token pt2 = new Token();
            String sPosList1 = "";
            String sPosList2 = "";
            while( it1.hasNext() && it2.hasNext() )
            {
                int compare = t1.word.compareTo( t2.word );
                if( compare == 0 )
                {
                    t1 = it1.next();
                    tc1++;
                    t2 = it2.next();
                    tc2++;
                }
                else if( compare < 0 )
                {
                    // Skip entity replacements
                    String entityNameValue = hmEntityNamesValues.get( t1.word );
                    if( entityNameValue == null )
                    {
                        issues++;
                        if( sPosList1.length() == 0 )
                            System.out.println( "File2 is missing ~" + t1.word + "~ consult File1 at " + t1.pos );
                        else
                            System.out.println( "File2 is missing ~" + t1.word + "~ consult File1 at " + sPosList1 + "," + t1.pos );
                    }
                    t1 = it1.next();
                    tc1++;
                }
                else
                {
                    // Skip entity replacements
                    String entityNameValue = hmEntityNamesValues.get( t2.word );
                    if( entityNameValue == null )
                    {
                        issues++;
                        if( sPosList2.length() == 0 )
                            System.out.println( "File1 is missing ~" + t2.word + "~ consult File2 at " + t2.pos );
                        else
                            System.out.println( "File1 is missing ~" + t2.word + "~ consult File2 at " + sPosList2 + "," + t2.pos );
                    }
                    t2 = it2.next();
                    tc2++;
                }
                if( pt1.word != null && pt1.word.compareTo( t1.word ) == 0 )
                {
                    if( sPosList1.length() == 0 )
                        sPosList1 = "" + pt1.pos;
                    else
                        sPosList1 += "," + pt1.pos;
                }
                else
                {
                    sPosList1 = "";
                }
                if( pt2.word != null && pt2.word.compareTo( t2.word ) == 0 )
                {
                    if( sPosList2.length() == 0 )
                        sPosList2 = "" + pt2.pos;
                    else
                        sPosList2 += "," + pt2.pos;
                }
                else
                {
                    sPosList2 = "";
                }
                pt1 = t1;
                pt2 = t2;
            }
        }
        catch( Exception ex )
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            ex.printStackTrace( pw );
            sw.toString();
            System.out.println( sw );
            System.out.println( "...Unsuccessful" );
            return;
        }
        System.out.println( "...Issues=" + issues );
    }
    
    /**
     * Main execution method for testing the {@link HtmlParser} class.
     * 
     * @param args command line arguments
     */
    public void run( String[] args )
    {
        if( args.length == 0 )
        {
            System.out.println( "Missing directory" );
            return;
        }
        
        HtmlParser hp = new HtmlParser();
//        hp.debugParser( true );
//        hp.debugValidate( true );
//        hp.configElemAttrLowerCase( true );
//        hp.configValidate( false );

        initHtmlLanguageExceptions();
        
        File dir = new File( args[ 0 ] );
        if( !dir.exists() )
        {
            System.out.println( "Invalid directory" );
            return;
        }
        
        String[] files = dir.list();
        for( int i = 0; i < files.length; i++ )
        {
            File f = new File( args[ 0 ] + "/" + files[ i ] );
            if( f.isFile() &&                
                f.getName().endsWith( ".html" ) ||
                f.getName().endsWith( ".htm" ) ||
                f.getName().endsWith( ".xhtml" ) )
            {
                String original = f.getAbsolutePath();
                System.out.println( "Processing... " + original );
                hp.parseFile( original );
                System.out.println( "parseFile (encoding=" + hp.getEncoding() + ") (" +
                                    "parse-items=" + hp.getNumParseItems() + ") (" +
                                    "parse-issues=" + hp.getNumParseIssues() + ")" );
                hp.writeParseItemsToFile( original + "--parse-items.out" );
                if( hp.getNumParseIssues() > 0 )
                {
                    hp.writeParseIssuesToFile( original + "--parse-issues.out" );
                    continue;
                }
                hp.writeCleanXmlToFile( original + "--clean.xml" );
                boolean xmlParsingSuccess = true;
                if( hp.getEncoding() == "UTF-8" )
                {
                    xmlParsingSuccess = verifyXmlParsing( original + "--clean.xml" );
                }
                else
                {
                    // Note: Charset not changed within html file
                    try
                    {
                        FileOutputStream fos = new FileOutputStream( original + "--clean-utf8.xml" );
                        byte[] BOM = new byte[ 3 ];
                        BOM[ 0 ] = ( byte )0xEF;
                        BOM[ 1 ] = ( byte )0xBB;
                        BOM[ 2 ] = ( byte )0xBF;
                        fos.write( BOM );
                        fos.write( jc.convertString( hp.getCleanXml(), "UTF-8" ).getBytes( "UTF-8" ) );
                        fos.close();
                    }
                    catch( Exception ex ) {}
                    xmlParsingSuccess = verifyXmlParsing( original + "--clean-utf8.xml" );
                }
                if( xmlParsingSuccess )
                    compareHtmlLanguage( hp.getEncoding(), original, original + "--clean.xml" );
            }
        }
    }

    /**
     * Application entry point.
     * 
     * @param args command line arguments
     */
    public static void main( String[] args )
    {
        TestHtmlParser thp = new TestHtmlParser();
        thp.run( args );
    }
}
