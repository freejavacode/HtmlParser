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

package freejavacode.parser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

/**
 * This class was the result of not finding an existing Java library that
 * could reasonably "parse" (or more appropriately "clean") an Internet
 * based HTML file so that it could be passed into Java's built-in XML
 * parser.  Other parsers either couldn't handle the input, or if they
 * could handle the input, didn't properly handle foreign character sets.
 * <p>
 * It takes two passes.  The first pass breaks everything into HTML
 * tokens.  The second pass makes sure the token arrangement is acceptable.
 * Along the way it cleans anything the XML parser will choke on.  It does
 * not "pretty" the file.  "Prettying" the file makes it difficult to
 * detect processing errors and is unnecessary for the XML parser destination.
 * 
 * @author Free Java Code
 *
 */
public class HtmlParser
{
    private static final byte TAG_OPEN = ( byte )'<';
    private static final byte TAG_CLOSE = ( byte )'>';
    private static final byte TAG_SLASH = ( byte )'/';
    private static final byte TAG_EXCLAMATION = ( byte )'!';
    private static final byte TAG_QUESTION = ( byte )'?';
    private static final byte TAG_OPEN_BRACKET = ( byte )'[';
    private static final byte TAG_CLOSE_BRACKET = ( byte )']';
    private static final byte TAG_DASH = ( byte )'-';
    
    private static final byte ATTR_SPACE = ( byte )' ';
    private static final byte ATTR_SPACE_TAB = ( byte )'\t';    
    private static final byte ATTR_SPACE_CR = ( byte )'\r';
    private static final byte ATTR_SPACE_LF = ( byte )'\n';
    private static final byte ATTR_EQUAL = ( byte )'=';
    private static final byte ATTR_QUOTE1 = ( byte )'\'';
    private static final byte ATTR_QUOTE2 = ( byte )'"';
    
    private static final char XML_AMPERSAND = '&';
    private static final char XML_SEMICOLON = ';';
    
    private static final String[] XML_DECLS = { "DOCTYPE", "ATTLIST", "ELEMENT", "ENTITY", "NOTATION" };
    
    /**
     * Hash map of XML entity names with their associated "valid XML"
     * substitutions.
     */
    public static final Map< String, String > mEntityNames;
    static
    {
        HashMap< String, String > hm = new HashMap< String, String >();
        hm.put( "quot", "quot" ); // XML supported
        hm.put( "amp", "amp" ); // XML supported
        hm.put( "apos", "#39" ); // XML supported, not handled in IE (number used) 
        hm.put( "lt", "lt" ); // XML supported
        hm.put( "gt", "gt" ); // XML supported
        hm.put( "nbsp", "#160" );
        hm.put( "iexcl", "#161" );
        hm.put( "cent", "#162" );
        hm.put( "pound", "#163" );
        hm.put( "curren", "#164" );
        hm.put( "yen", "#165" );
        hm.put( "brvbar", "#166" );
        hm.put( "sect", "#167" );
        hm.put( "uml", "#168" );
        hm.put( "copy", "#169" );
        hm.put( "ordf", "#170" );
        hm.put( "laquo", "#171" );
        hm.put( "not", "#172" );
        hm.put( "shy", "#173" );
        hm.put( "reg", "#174" );
        hm.put( "macr", "#175" );
        hm.put( "deg", "#176" );
        hm.put( "plusmn", "#177" );
        hm.put( "sup2", "#178" );
        hm.put( "sup3", "#179" );
        hm.put( "acute", "#180" );
        hm.put( "micro", "#181" );
        hm.put( "para", "#182" );
        hm.put( "middot", "#183" );
        hm.put( "cedil", "#184" );
        hm.put( "sup1", "#185" );
        hm.put( "ordm", "#186" );
        hm.put( "raquo", "#187" );
        hm.put( "frac14", "#188" );
        hm.put( "frac12", "#189" );
        hm.put( "frac34", "#190" );
        hm.put( "iquest", "#191" );
        hm.put( "Agrave", "#192" );
        hm.put( "Aacute", "#193" );
        hm.put( "Acirc", "#194" );
        hm.put( "Atilde", "#195" );
        hm.put( "Auml", "#196" );
        hm.put( "Aring", "#197" );
        hm.put( "AElig", "#198" );
        hm.put( "Ccedil", "#199" );
        hm.put( "Egrave", "#200" );
        hm.put( "Eacute", "#201" );
        hm.put( "Ecirc", "#202" );
        hm.put( "Euml", "#203" );
        hm.put( "Igrave", "#204" );
        hm.put( "Iacute", "#205" );
        hm.put( "Icirc", "#206" );
        hm.put( "Iuml", "#207" );
        hm.put( "ETH", "#208" );
        hm.put( "Ntilde", "#209" );
        hm.put( "Ograve", "#210" );
        hm.put( "Oacute", "#211" );
        hm.put( "Ocirc", "#212" );
        hm.put( "Otilde", "#213" );
        hm.put( "Ouml", "#214" );
        hm.put( "times", "#215" );
        hm.put( "Oslash", "#216" );
        hm.put( "Ugrave", "#217" );
        hm.put( "Uacute", "#218" );
        hm.put( "Ucirc", "#219" );
        hm.put( "Uuml", "#220" );
        hm.put( "Yacute", "#221" );
        hm.put( "THORN", "#222" );
        hm.put( "szlig", "#223" );
        hm.put( "agrave", "#224" );
        hm.put( "aacute", "#225" );
        hm.put( "acirc", "#226" );
        hm.put( "atilde", "#227" );
        hm.put( "auml", "#228" );
        hm.put( "aring", "#229" );
        hm.put( "aelig", "#230" );
        hm.put( "ccedil", "#231" );
        hm.put( "egrave", "#232" );
        hm.put( "eacute", "#233" );
        hm.put( "ecirc", "#234" );
        hm.put( "euml", "#235" );
        hm.put( "igrave", "#236" );
        hm.put( "iacute", "#237" );
        hm.put( "icirc", "#238" );
        hm.put( "iuml", "#239" );
        hm.put( "eth", "#240" );
        hm.put( "ntilde", "#241" );
        hm.put( "ograve", "#242" );
        hm.put( "oacute", "#243" );
        hm.put( "ocirc", "#244" );
        hm.put( "otilde", "#245" );
        hm.put( "ouml", "#246" );
        hm.put( "divide", "#247" );
        hm.put( "oslash", "#248" );
        hm.put( "ugrave", "#249" );
        hm.put( "uacute", "#250" );
        hm.put( "ucirc", "#251" );
        hm.put( "uuml", "#252" );
        hm.put( "yacute", "#253" );
        hm.put( "thorn", "#254" );
        hm.put( "yuml", "#255" );
        hm.put( "OElig", "#338" );
        hm.put( "oelig", "#339" );
        hm.put( "Scaron", "#352" );
        hm.put( "scaron", "#353" );
        hm.put( "Yuml", "#376" );
        hm.put( "fnof", "#402" );
        hm.put( "circ", "#710" );
        hm.put( "tilde", "#732" );
        hm.put( "Alpha", "#913" );
        hm.put( "Beta", "#914" );
        hm.put( "Gamma", "#915" );
        hm.put( "Delta", "#916" );
        hm.put( "Epsilon", "#917" );
        hm.put( "Zeta", "#918" );
        hm.put( "Eta", "#919" );
        hm.put( "Theta", "#920" );
        hm.put( "Iota", "#921" );
        hm.put( "Kappa", "#922" );
        hm.put( "Lambda", "#923" );
        hm.put( "Mu", "#924" );
        hm.put( "Nu", "#925" );
        hm.put( "Xi", "#926" );
        hm.put( "Omicron", "#927" );
        hm.put( "Pi", "#928" );
        hm.put( "Rho", "#929" );
        hm.put( "Sigma", "#931" );
        hm.put( "Tau", "#932" );
        hm.put( "Upsilon", "#933" );
        hm.put( "Phi", "#934" );
        hm.put( "Chi", "#935" );
        hm.put( "Psi", "#936" );
        hm.put( "Omega", "#937" );
        hm.put( "alpha", "#945" );
        hm.put( "beta", "#946" );
        hm.put( "gamma", "#947" );
        hm.put( "delta", "#948" );
        hm.put( "epsilon", "#949" );
        hm.put( "zeta", "#950" );
        hm.put( "eta", "#951" );
        hm.put( "theta", "#952" );
        hm.put( "iota", "#953" );
        hm.put( "kappa", "#954" );
        hm.put( "lambda", "#955" );
        hm.put( "mu", "#956" );
        hm.put( "nu", "#957" );
        hm.put( "xi", "#958" );
        hm.put( "omicron", "#959" );
        hm.put( "pi", "#960" );
        hm.put( "rho", "#961" );
        hm.put( "sigmaf", "#962" );
        hm.put( "sigma", "#963" );
        hm.put( "tau", "#964" );
        hm.put( "upsilon", "#965" );
        hm.put( "phi", "#966" );
        hm.put( "chi", "#967" );
        hm.put( "psi", "#968" );
        hm.put( "omega", "#969" );
        hm.put( "thetasym", "#977" );
        hm.put( "upsih", "#978" );
        hm.put( "piv", "#982" );
        hm.put( "ensp", "#8194" );
        hm.put( "emsp", "#8195" );
        hm.put( "thinsp", "#8201" );
        hm.put( "zwnj", "#8204" );
        hm.put( "zwj", "#8205" );
        hm.put( "lrm", "#8206" );
        hm.put( "rlm", "#8207" );
        hm.put( "ndash", "#8211" );
        hm.put( "mdash", "#8212" );
        hm.put( "lsquo", "#8216" );
        hm.put( "rsquo", "#8217" );
        hm.put( "sbquo", "#8218" );
        hm.put( "ldquo", "#8220" );
        hm.put( "rdquo", "#8221" );
        hm.put( "bdquo", "#8222" );
        hm.put( "dagger", "#8224" );
        hm.put( "Dagger", "#8225" );
        hm.put( "bull", "#8226" );
        hm.put( "hellip", "#8230" );
        hm.put( "permil", "#8240" );
        hm.put( "prime", "#8242" );
        hm.put( "Prime", "#8243" );
        hm.put( "lsaquo", "#8249" );
        hm.put( "rsaquo", "#8250" );
        hm.put( "oline", "#8254" );
        hm.put( "frasl", "#8260" );
        hm.put( "euro", "#8364" );
        hm.put( "image", "#8465" );
        hm.put( "weierp", "#8472" );
        hm.put( "real", "#8476" );
        hm.put( "trade", "#8482" );
        hm.put( "alefsym", "#8501" );
        hm.put( "larr", "#8592" );
        hm.put( "uarr", "#8593" );
        hm.put( "rarr", "#8594" );
        hm.put( "darr", "#8595" );
        hm.put( "harr", "#8596" );
        hm.put( "crarr", "#8629" );
        hm.put( "lArr", "#8656" );
        hm.put( "uArr", "#8657" );
        hm.put( "rArr", "#8658" );
        hm.put( "dArr", "#8659" );
        hm.put( "hArr", "#8660" );
        hm.put( "forall", "#8704" );
        hm.put( "part", "#8706" );
        hm.put( "exist", "#8707" );
        hm.put( "empty", "#8709" );
        hm.put( "nabla", "#8711" );
        hm.put( "isin", "#8712" );
        hm.put( "notin", "#8713" );
        hm.put( "ni", "#8715" );
        hm.put( "prod", "#8719" );
        hm.put( "sum", "#8721" );
        hm.put( "minus", "#8722" );
        hm.put( "lowast", "#8727" );
        hm.put( "radic", "#8730" );
        hm.put( "prop", "#8733" );
        hm.put( "infin", "#8734" );
        hm.put( "ang", "#8736" );
        hm.put( "and", "#8743" );
        hm.put( "or", "#8744" );
        hm.put( "cap", "#8745" );
        hm.put( "cup", "#8746" );
        hm.put( "int", "#8747" );
        hm.put( "there4", "#8756" );
        hm.put( "sim", "#8764" );
        hm.put( "cong", "#8773" );
        hm.put( "asymp", "#8776" );
        hm.put( "ne", "#8800" );
        hm.put( "equiv", "#8801" );
        hm.put( "le", "#8804" );
        hm.put( "ge", "#8805" );
        hm.put( "sub", "#8834" );
        hm.put( "sup", "#8835" );
        hm.put( "nsub", "#8836" );
        hm.put( "sube", "#8838" );
        hm.put( "supe", "#8839" );
        hm.put( "oplus", "#8853" );
        hm.put( "otimes", "#8855" );
        hm.put( "perp", "#8869" );
        hm.put( "sdot", "#8901" );
        hm.put( "lceil", "#8968" );
        hm.put( "rceil", "#8969" );
        hm.put( "lfloor", "#8970" );
        hm.put( "rfloor", "#8971" );
        hm.put( "lang", "#10216" );
        hm.put( "rang", "#10217" );
        hm.put( "loz", "#9674" );
        hm.put( "spades", "#9824" );
        hm.put( "clubs", "#9827" );
        hm.put( "hearts", "#9829" );
        hm.put( "diams", "#9830" );
        mEntityNames = Collections.unmodifiableMap( hm );
    };
    
    /**
     * Hash map of "void elements" for HTML 4.01 and HTML 5.
     */
    public static final Map< String, String > mVoidElements;
    static
    {
        HashMap< String, String > hm = new HashMap< String, String >();
        // HTML 4.01:
        //  area, base, basefont, br, col, frame, hr, img, input, isindex,
        //  link, meta, param
        // HTML 5:
        //  area, base, br, col, command, embed, hr, img, input, keygen,
        //  link, meta, param, source, track, wbr
        hm.put( "area", "area" );
        hm.put( "base", "base" );
        hm.put( "basefont", "basefont" );
        hm.put( "br", "br" );
        hm.put( "col", "col" );
        hm.put( "command", "command" );
        hm.put( "embed", "embed" );
        hm.put( "frame", "frame" );
        hm.put( "hr", "hr" );
        hm.put( "img", "img" );
        hm.put( "input", "input" );
        hm.put( "isindex", "isindex" );
        hm.put( "keygen", "keygen" );
        hm.put( "link", "link" );
        hm.put( "meta", "meta" );
        hm.put( "param", "param" );
        hm.put( "source", "source" );
        hm.put( "track", "track" );
        hm.put( "wr", "wr" );
        mVoidElements = Collections.unmodifiableMap( hm );
    };

    private enum HtmlPartType
    {
        TAG_START, // < >
        TAG_EMPTY, // < />
        TAG_END, // </ >
        TAG_DECL, // <! >
        TAG_DECL2, // <![ ]]>
        TAG_PI, // <? ?>
        TAG_COMMENT, // <!-- -->
        ATTR_NAME,
        ATTR_VALUE,
        ATTR_SOLO, // "name" without "value"
        TEXT,
        TEXT_SCRIPT,
        TEXT_STYLE
    };

    private class HtmlPart
    {
        public HtmlPartType type;
        public String value;
        public int offset;
        public int level = 0;
    };
    
    private ArrayList< HtmlPart > alItems = new ArrayList< HtmlPart >();
    private ArrayList< String > alIssues = new ArrayList< String >();

    private JavaCharsets jc = new JavaCharsets();
    private String encodingTags = "UTF-8";
    private String encodingText = "UTF-8";
    private boolean encodingFound = false;
    private boolean encodingRestart = true;
    
    private ByteBuffer bb;
    private byte b;
    
    private boolean debugParser = false;
    private boolean debugValidate = false;
    private boolean configValidate = true;
    private boolean configElemAttrLowerCase = false;
    
    private String doctypeRootElement = "html"; // Needed for DOCTYPE declaration
    
    /**
     * Does nothing.
     */
    public HtmlParser()
    {
    }
    
    /**
     * Flag to dump out debug information during "pass one" (breaking into
     * tokens).  Call before the "parse()" methods.
     * 
     * @param debug pass true or false (default: false)
     */
    public void debugParser( boolean debug )
    {
        debugParser = debug;
    }
    
    /**
     * Flag to dump out debug information during "pass two" (token
     * arrangement).  Call before the "parse()" methods.
     * 
     * @param debug pass true or false (default: false)
     */
    public void debugValidate( boolean debug )
    {
        debugValidate = debug;
    }
    
    /**
     * Flag to turn on/off "pass two" (token arrangement).
     * Call before the "parse()" methods.
     * 
     * @param validate pass true or false (default: true)
     */
    public void configValidate( boolean validate )
    {
        configValidate = validate; 
    }
    
    /**
     * Flag to force elements and attributes to lower case.
     * Call before the "parse()" methods.
     * 
     * @param lowerCase pass true or false (default: false)
     */
    public void configElemAttrLowerCase( boolean lowerCase )
    {
        configElemAttrLowerCase = lowerCase;
    }
    
    private String grabString( int pos0, int pos1, String encoding )
    {
        bb.position( pos0 - 1 );
        byte[] bytes = new byte[ pos1 - pos0 ];
        bb.get( bytes, 0, pos1 - pos0 );
        bb.position( pos1 - 1 );
        b = bb.get();
        String s = "";
        try
        {
            s = new String( bytes, encoding );
        }
        catch( Exception ex ) {}
        
        if( debugParser )
            System.out.println( "" + pos0 + " [" + s + "]" );
        
        return( s );
    }
    
    private boolean isWhiteSpace( byte bb )
    {
        return( bb == ATTR_SPACE ||
                bb == ATTR_SPACE_TAB ||
                bb == ATTR_SPACE_CR ||
                bb == ATTR_SPACE_LF );
    }
    
    private boolean findClose()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( b == TAG_CLOSE )
                return( true );
        }
        return( false );
    }
    
    private boolean findCloseBracketTwiceAndClose()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( b == TAG_CLOSE_BRACKET )
            {
                b = bb.get();
                if( b == TAG_CLOSE_BRACKET )
                {
                    b = bb.get();
                    if( b == TAG_CLOSE )
                        return( true );
                }
            }
        }
        return( false );
    }
    
    private boolean findQuestionAndClose()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( b == TAG_QUESTION )
            {
                b = bb.get();
                if( b == TAG_CLOSE )
                    return( true );
            }
        }
        return( false );
    }
    
    private boolean findDashAndDashAndClose()
    {
        while( bb.position() < bb.limit() )
        {
            if( b == TAG_DASH )
            {
                b = bb.get();
                if( b == TAG_DASH )
                {
                    b = bb.get();
                    if( b == TAG_CLOSE )
                        return( true );
                }
            }
            b = bb.get();
        }
        return( false );
    }
    
    private boolean findSpaceOrSlashOrClose()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( isWhiteSpace( b ) ||
                b == TAG_SLASH ||
                b == TAG_CLOSE )
                return( true );
        }
        return( false );
    }
    
    private boolean findSpaceOrEqualOrSlashOrClose()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( isWhiteSpace( b ) ||
                b == ATTR_EQUAL ||
                b == TAG_SLASH ||
                b == TAG_CLOSE )
                return( true );
        }
        return( false );
    }
    
    private boolean findQuoteOrClose( byte quote )
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( b == quote ||
                b == TAG_CLOSE )
                return( true );
        }
        return( false );
    }
    
    private boolean findSpaceOrClose()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( isWhiteSpace( b ) ||
                b == TAG_CLOSE )
                return( true );
        }
        return( false );
    }
    
    private boolean findNonSpace()
    {
        while( bb.position() < bb.limit() )
        {
            b = bb.get();
            if( !isWhiteSpace( b ) )
                return( true );
        }
        return( false );
    }
    
    private boolean hasEncodingDirective( String value, String search )
    {
        if( encodingFound )
            return( false );
        
        int index0 = value.indexOf( search );
        if( index0 == -1 )
            return( false );
        
        byte b0 = ATTR_QUOTE1;
        index0 += search.length(); 
        for( ; index0 < value.length(); index0++ )
        {
            b0 = ( byte )value.charAt( index0 );
            if( b0 == ATTR_QUOTE1 || b0 == ATTR_QUOTE2 )
            {
                index0++;
                break;
            }
            if( !isWhiteSpace( b0 ) )
                break;
        }
        byte b1;
        int index1 = index0;
        for( ; index1 < value.length(); index1++ )
        {
            b1 = ( byte )value.charAt( index1 );
            if( b0 == ATTR_QUOTE1 && b1 == ATTR_QUOTE1 ||
                b0 == ATTR_QUOTE2 && b1 == ATTR_QUOTE2 ||
                !isWhiteSpace( b0 ) && isWhiteSpace( b1 ) )
                break;
        }
        String found = value.substring( index0, index1 );
        String encoding = jc.getCharset( found ).name();
        encodingRestart = !encoding.equals( encodingText );
        encodingText = encoding;
        return( true );
    }
    
    private String translateSpecialChars( String value )
    {
        String s = "";
        boolean javascript = false;
        char[] ach = value.toCharArray();
        int cpCount = Character.codePointCount( ach, 0, ach.length );
        int[] acp = new int[ 1 ];
        for( int i = 0; i < cpCount; )
        {
            int cp = Character.codePointAt( ach, i );
            int chCount = Character.charCount( cp );
            acp[ 0 ] = cp;
            String scp = new String( acp, 0, 1 );
            if( cp == XML_AMPERSAND )
            {
                if( javascript )
                {
                    s += "&amp;";
                    i += chCount;
                    continue;
                }
                boolean convert = true;
                String s2 = "";
                for( int j = i + 1; j < cpCount; )
                {
                    int cp2 = Character.codePointAt( ach, j );
                    int chCount2 = Character.charCount( cp2 );
                    if( cp2 == XML_AMPERSAND )
                    {
                        break;
                    }
                    else if( cp2 == XML_SEMICOLON )
                    {
                        // Handle the known ones and change any others to "nbsp"
                        i = j;
                        if( s2.startsWith( "#" ) )
                        {
                            boolean validNumber = true;
                            for( int k = 1; k < s2.length(); k++ )
                            {
                                if( !Character.isDigit( s2.charAt( k ) ) )
                                {
                                    validNumber = false;
                                    break;
                                }
                            }
                            if( !validNumber )
                                break;
                            s += "&" + s2 + ";";
                        }
                        else
                        {
                            String s3 = mEntityNames.get( s2 );
                            if( s3 != null )
                                s += "&" + s3 + ";";
                            else
                                s += "&#160;"; // nbsp
                        }
                        convert = false;
                        break;
                    }
                    else
                    {
                        acp[ 0 ] = cp2;
                        String scp2 = new String( acp, 0, 1 );
                        s2 += scp2;
                    }
                    j += chCount2;
                }
                if( convert )
                {
                    s += "&amp;";
                }
            }
            else if( cp == XML_SEMICOLON )
            {
                // Is there a base64 encoded image?
                int loop = 0;
                for( int j = i + 1; j < cpCount; )
                {
                    int cp2 = Character.codePointAt( ach, j );
                    int chCount2 = Character.charCount( cp2 );
                    if( loop == 0 && cp2 != 'b' )
                        break;
                    if( loop == 1 && cp2 != 'a' )
                        break;
                    if( loop == 2 && cp2 != 's' )
                        break;
                    if( loop == 3 && cp2 != 'e' )
                        break;
                    if( loop == 4 && cp2 != '6' )
                        break;
                    if( loop == 5 && cp2 == '4' )
                    {
                        s += new String( ach, i, ach.length - i );
                        return( s );
                    }
                    j += chCount2;
                    loop++;
                }

                s += scp;
                
                // It's javascript, different conversion
                javascript = true;
            }
            else if( cp == TAG_OPEN )
            {
                String s3 = mEntityNames.get( "lt" );
                if( s3 != null )
                    s += "&" + s3 + ";";
                else
                    s += "&#160;"; // nbsp
            }
            else if( cp == TAG_CLOSE )
            {
                String s3 = mEntityNames.get( "gt" );
                if( s3 != null )
                    s += "&" + s3 + ";";
                else
                    s += "&#160;"; // nbsp
            }
            // Translate invalid XML characters into space
            // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
            else if( !( cp == 0x09 || cp == 0x0A || cp == 0x0D ||
                        ( cp >= 0x20 && cp <= 0xD7FF ) ||
                        ( cp >= 0xE000 && cp <= 0xFFFD ) ||
                        ( cp >= 0x10000 && cp <= 0x10FFFF ) ) )
            {
                s += " ";
            }
            else
            {
                s += scp;
            }
            i += chCount;
        }
        return( s );
    }

    private boolean parseRestart()
    {
        if( encodingFound )
            return( false );
        encodingFound = true;
        if( !encodingRestart )
            return( false );
        alItems.clear();
        if( bb != null )
            bb.rewind();
        b = 0;
        return( true );
    }
    
    private void parseReset()
    {
        alItems.clear();
        alIssues.clear();
        encodingText = encodingTags;
        encodingFound = false;
        if( bb != null )
            bb.clear();
        b = 0;        
    }
    
    // Basic check: Starts with a letter, no standard parsing chars
    // Additional: If first character is TAG_OPEN, close tag, move on
    private int xmlNameValidity( String s )
    {
        if( s == null )
            return( -1 );
        if( s.charAt( 0 ) == TAG_OPEN )
            return( 0 );
        char[] ach = s.toCharArray();
        int cp = Character.codePointAt( ach, 0 );
        if( cp > 0xFF ||
            !Character.isLetter( cp ) ||
            s.indexOf( ATTR_QUOTE1 ) != -1 ||
            s.indexOf( ATTR_QUOTE2 ) != -1 ||
            s.indexOf( ATTR_EQUAL ) != -1 ||
            s.indexOf( TAG_OPEN ) != -1 ||
            s.indexOf( TAG_CLOSE ) != -1 )   
            return( -1 );
        return( 1 );
    }
    
    // Java XML Parser complains if there's a "--" inside the comment.
    // Actually, it can't handle a "-" right next to the end comment either "--->",
    // even though there's only one extra "-" (still complains there's two).
    // Just changing all of them to "_".
    private String cleanComment( String s )
    {
        return( s.replace( '-', '_' ) );
    }
    
    private void parse( boolean encodingOnly )
    {
        int tpos0 = b + 1;
        HtmlPart thp = new HtmlPart();
        boolean ignoreScriptStyle = false;
        boolean collectingText = false;
        String prevTag = "";
        while( bb.position() < bb.limit() )
        {
            if( b == TAG_CLOSE &&
                !ignoreScriptStyle &&
                !collectingText )
            {
                tpos0 = bb.position() + 1;
                collectingText = true;
            }
            b = bb.get();
            if( b == TAG_OPEN )
            {
                int tpos1 = bb.position();
                thp = new HtmlPart();
                thp.type = HtmlPartType.TEXT;
                if( tpos1 - tpos0 > 0 )
                    thp.value = grabString( tpos0, tpos1, encodingText );
                else
                    thp.value = "";
                thp.offset = tpos0;
                if( !ignoreScriptStyle )
                {
                    thp.value = translateSpecialChars( thp.value );
                    alItems.add( thp );
                    collectingText = false;
                }
                b = bb.get();
                if( b == TAG_EXCLAMATION &&
                    !ignoreScriptStyle )
                {
                    b = bb.get();
                    if( b == TAG_OPEN_BRACKET )
                    {
                        b = bb.get();
                        HtmlPart d2hp = new HtmlPart();
                        d2hp.type = HtmlPartType.TAG_DECL2;
                        int pos0 = bb.position();
                        boolean ret = findCloseBracketTwiceAndClose();
                        int pos1 = bb.position() - 2;
                        if( ret )
                        {
                            d2hp.value = grabString( pos0, pos1, encodingTags );
                            d2hp.offset = pos0;
                            alItems.add( d2hp );
                        }
                        else
                        {
                            alIssues.add( "Invalid xml include declaration at " + pos0 );
                            bb.position( pos0 );
                            continue;
                        }
                    }
                    else if( b == TAG_DASH )
                    {
                        b = bb.get();
                        if( b == TAG_DASH )
                        {
                            b = bb.get();
                            HtmlPart chp = new HtmlPart();
                            chp.type = HtmlPartType.TAG_COMMENT;
                            int pos0 = bb.position();
                            boolean ret = findDashAndDashAndClose();
                            int pos1 = bb.position() - 2;
                            if( ret )
                            {
                                String comment = grabString( pos0, pos1, encodingText );
                                chp.value = cleanComment( comment );
                                chp.offset = pos0;
                                alItems.add( chp );
                                b = bb.get();
                                b = bb.get();
                            }
                            else
                            {
                                alIssues.add( "Invalid xml comment at " + pos0 );
                                bb.position( pos0 );
                                continue;
                            }
                        }
                        else
                        {
                            alIssues.add( "Invalid xml comment at " + bb.position() );
                            continue;
                        }
                    }
                    else
                    {
                        HtmlPart dhp = new HtmlPart();
                        dhp.type = HtmlPartType.TAG_DECL;
                        int pos0 = bb.position();
                        boolean ret = findClose();
                        int pos1 = bb.position();
                        if( ret )
                        {
                            dhp.value = grabString( pos0, pos1, encodingTags );
                            boolean validXmlDecl = false;
                            for( int i = 0; i < XML_DECLS.length; i++ )
                            {
                                if( dhp.value.toUpperCase().startsWith( XML_DECLS[ i ] ) )
                                {
                                    validXmlDecl = true;
                                    break;
                                }
                            }
                            if( !validXmlDecl )
                                dhp.type = HtmlPartType.TAG_COMMENT;
                            dhp.offset = pos0;
                            alItems.add( dhp );
                        }
                        else
                        {
                            alIssues.add( "Invalid xml declaration at " + pos0 );
                            bb.position( pos0 );
                            continue;
                        }
                    }
                }
                else if( b == TAG_QUESTION &&
                         !ignoreScriptStyle )
                {
                    b = bb.get();
                    HtmlPart pihp = new HtmlPart();
                    pihp.type = HtmlPartType.TAG_PI;
                    int pos0 = bb.position();
                    boolean ret = findQuestionAndClose();
                    int pos1 = bb.position() - 1;
                    if( ret )
                    {
                        pihp.value = grabString( pos0, pos1, encodingTags );
                        pihp.offset = pos0;
                        // Check for encoding directive
                        if( hasEncodingDirective( pihp.value, " encoding=" ) )
                        {
                            if( encodingOnly )
                                return;
                            
                            // Restart parse with new text encoding, if not already found
                            if( parseRestart() )
                                continue;
                        }
                        alItems.add( pihp );
                    }
                    else
                    {
                        alIssues.add( "Invalid xml processing instruction at " + pos0 );
                        bb.position( pos0 );
                        continue;
                    }
                }
                else if( b == TAG_SLASH )
                {
                    b = bb.get();
                    HtmlPart ethp = new HtmlPart();
                    ethp.type = HtmlPartType.TAG_END;
                    int pos0 = bb.position();
                    boolean ret = findClose();
                    int pos1 = bb.position();
                    if( ret )
                    {
                        ethp.value = grabString( pos0, pos1, encodingTags );
                        int nameValidity = xmlNameValidity( ethp.value );
                        if( nameValidity != 1 )
                            ethp.value = "InvalidXmlName";
                        if( ignoreScriptStyle )
                        {
                            if( ethp.value.toLowerCase( Locale.ENGLISH ).equals( "script" ) ||
                                ethp.value.toLowerCase( Locale.ENGLISH ).equals( "style" ) )
                            {
                                if( ethp.value.toLowerCase( Locale.ENGLISH ).equals( "script" ) )
                                {
                                    thp.type = HtmlPartType.TEXT_SCRIPT;
                                    
                                    // Need to clean up comments if embedded in script tag
                                    int commentStartIndex = 0;
                                    while( commentStartIndex != -1 )
                                    {
                                        commentStartIndex = thp.value.indexOf( "<!--", commentStartIndex );
                                        if( commentStartIndex == -1 )
                                            break;
                                        int commentEndIndex = thp.value.indexOf( "-->", commentStartIndex );
                                        if( commentEndIndex == -1 )
                                            break;
                                        String comment = thp.value.substring( commentStartIndex + 4,
                                                                                commentEndIndex );
                                        thp.value = thp.value.replace( comment, cleanComment( comment ) );
                                        commentStartIndex = commentEndIndex + 3;
                                    }
                                }
                                else
                                    thp.type = HtmlPartType.TEXT_STYLE;
                                alItems.add( thp );
                                ignoreScriptStyle = false;
                            }
                            else
                            {
                                continue;
                            }
                        }
                        ethp.offset = pos0;
                        alItems.add( ethp );
                    }
                    else
                    {
                        alIssues.add( "Invalid end tag at " + pos0 );
                        bb.position( pos0 );
                        continue;
                    }
                }
                else
                {
                    if( ignoreScriptStyle )
                        continue;
                    HtmlPart sthp = new HtmlPart();
                    sthp.type = HtmlPartType.TAG_START;
                    int pos0 = bb.position();
                    boolean ret = findSpaceOrSlashOrClose();
                    int pos1 = bb.position();
                    if( ret )
                    {
                        sthp.value = grabString( pos0, pos1, encodingTags );
                        String invalidXmlName = "";
                        int nameValidity = xmlNameValidity( sthp.value );
                        if( nameValidity != 1 )
                        {
                            invalidXmlName = sthp.value;
                            sthp.value = "InvalidXmlName";
                        }
                        if( mVoidElements.get( sthp.value.toLowerCase( Locale.ENGLISH ) ) != null )
                            sthp.type = HtmlPartType.TAG_EMPTY;
                        prevTag = sthp.value.toLowerCase( Locale.ENGLISH );
                        sthp.offset = pos0;
                        if( isWhiteSpace( b ) )
                        {
                            ret = findNonSpace();
                            if( ret )
                            {
                                alItems.add( sthp );
                            }
                            else
                            {
                                alIssues.add( "Invalid start tag at " + pos0 );
                                bb.position( pos0 );
                                continue;
                            }
                        }
                        else
                        {
                            alItems.add( sthp );
                            if( invalidXmlName.length() > 0 )
                            {
                                String value = invalidXmlName;
                                value = translateSpecialChars( value );
                                HtmlPart anhp = new HtmlPart();
                                anhp.value = "InvalidXmlName_" + pos0 + "_" + pos1;
                                anhp.type = HtmlPartType.ATTR_NAME;
                                anhp.offset = pos0;
                                alItems.add( anhp );
                                HtmlPart avhp = new HtmlPart();
                                avhp.value = value;
                                avhp.type = HtmlPartType.ATTR_VALUE;
                                avhp.offset = pos0;
                                alItems.add( avhp );
                            }
                        }
                        if( b == TAG_SLASH )
                        {
                            sthp.type = HtmlPartType.TAG_EMPTY;
                            b = bb.get();
                        } 
                        boolean continueOuterLoop = false;
                        while( b != TAG_CLOSE )
                        {
                            HtmlPart anhp = new HtmlPart();
                            anhp.type = HtmlPartType.ATTR_NAME;
                            pos0 = bb.position();
                            ret = findSpaceOrEqualOrSlashOrClose();
                            pos1 = bb.position();
                            if( ret )
                            {
                                anhp.value = grabString( pos0, pos1, encodingTags );
                                nameValidity = xmlNameValidity( anhp.value );
                                if( nameValidity == 0 )
                                {
                                    // Got unexpected TAG_OPEN
                                    // Move back one before name
                                    // Simulate TAG_CLOSE
                                    bb.position( pos0 - 1 );
                                    b = TAG_CLOSE;
                                    break;
                                }
                                else if( nameValidity == -1 )
                                {
                                    String value = anhp.value;
                                    value = translateSpecialChars( value );
                                    value = value.replace( "\"", "_" );
                                    value = value.replace( "'", "_" );
                                    anhp.value = "InvalidXmlName_" + pos0 + "_" + pos1;
                                    anhp.type = HtmlPartType.ATTR_NAME;
                                    anhp.offset = pos0;
                                    alItems.add( anhp );
                                    HtmlPart avhp2 = new HtmlPart();
                                    avhp2.value = value;
                                    avhp2.type = HtmlPartType.ATTR_VALUE;
                                    avhp2.offset = pos0;
                                    alItems.add( avhp2 );
                                    if( b == TAG_SLASH )
                                    {
                                        sthp.type = HtmlPartType.TAG_EMPTY;
                                        b = bb.get();
                                    }
                                    if( b == TAG_CLOSE )
                                        continue;
                                    ret = findNonSpace();
                                    if( ret )
                                    {
                                        if( b == TAG_SLASH )
                                        {
                                            sthp.type = HtmlPartType.TAG_EMPTY;
                                            b = bb.get();
                                        }
                                        continue;                                        
                                    }
                                    else
                                    {
                                        alIssues.add( "Invalid attribute name at " + pos0 );
                                        bb.position( pos0 );
                                        break;
                                    }
                                }
                                anhp.offset = pos0;
                                if( isWhiteSpace( b ) )
                                {
                                    ret = findNonSpace();
                                    if( ret )
                                    {
                                        alItems.add( anhp );
                                    }
                                    else
                                    {
                                        alIssues.add( "Invalid attribute name at " + pos0 );
                                        bb.position( pos0 );
                                        break;
                                    }
                                }
                                else
                                {
                                    alItems.add( anhp );
                                }
                                if( b == ATTR_EQUAL )
                                {
                                    ret = findNonSpace();
                                    if( ret )
                                    {
                                        if( b == TAG_SLASH )
                                        {
                                            sthp.type = HtmlPartType.TAG_EMPTY;
                                            b = bb.get();
                                        }
                                        if( b == TAG_CLOSE )
                                        {
                                            HtmlPart avhp = new HtmlPart();
                                            avhp.type = HtmlPartType.ATTR_VALUE;
                                            avhp.value = "";
                                            avhp.offset = bb.position();
                                            alItems.add( avhp );
                                            continue;
                                        }
                                    }
                                    else
                                    {
                                        alIssues.add( "Invalid attribute value at " + pos0 );
                                        bb.position( pos0 );
                                        break; 
                                    }
                                }
                                else
                                {
                                    anhp.type = HtmlPartType.ATTR_SOLO;
                                    continue;
                                }
                            }
                            else
                            {
                                alIssues.add( "Invalid attribute name at " + pos0 );
                                bb.position( pos0 );
                                break;
                            }
                            HtmlPart avhp = new HtmlPart();
                            avhp.type = HtmlPartType.ATTR_VALUE;
                            pos0 = bb.position();
                            byte quote = 0;
                            if( b == ATTR_QUOTE1 || b == ATTR_QUOTE2 )
                            {
                                pos0 = bb.position() + 1;
                                quote = b;
                                ret = findQuoteOrClose( quote );
                            }
                            else
                            {
                                ret = findSpaceOrClose();
                            }
                            pos1 = bb.position();
                            if( ret )
                            {
                                if( quote == 0 && b == TAG_CLOSE )
                                {
                                    bb.position( pos1 - 1 );
                                    b = bb.get();
                                    if( b == TAG_SLASH )
                                    {
                                        pos1--;
                                        bb.get();
                                    }
                                }
                                avhp.value = grabString( pos0, pos1, encodingText );
                                avhp.offset = pos0;
                                // Check for encoding directive
                                if( prevTag.toLowerCase( Locale.ENGLISH ).equals( "meta" ) &&
                                    hasEncodingDirective( avhp.value, " charset=" ) )
                                {
                                    if( encodingOnly )
                                        return;
                                    
                                    // Restart parse with new text encoding, if necessary
                                    if( parseRestart() )
                                    {
                                        continueOuterLoop = true;
                                        break; // break loop first, then continue;
                                    }
                                }
                                avhp.value = translateSpecialChars( avhp.value );
                                alItems.add( avhp );
                                if( b == TAG_CLOSE )
                                    continue;
                            }
                            else
                            {
                                alIssues.add( "Invalid attribute value at " + pos0 );
                                bb.position( pos0 );
                                break;
                            }
                            ret = findNonSpace();
                            if( ret )
                            {
                                if( b == TAG_SLASH )
                                {
                                    sthp.type = HtmlPartType.TAG_EMPTY;
                                    b = bb.get();
                                }
                                continue;
                            }
                            else
                            {
                                alIssues.add( "Invalid attribute name at " + pos0 );
                                bb.position( pos0 );
                                break;
                            }
                        }
                        if( continueOuterLoop )
                            continue; // Restart parse with new text encoding
                        if( sthp.value.toLowerCase( Locale.ENGLISH ).equals( "script" ) ||
                            sthp.value.toLowerCase( Locale.ENGLISH ).equals( "style" ) )
                        {
                            ignoreScriptStyle = true;
                            tpos0 = bb.position() + 1;
                        }
                    }
                    else
                    {
                        alIssues.add( "Invalid start or empty tag at " + pos0 );
                        bb.position( pos0 );
                        continue;
                    }
                }
            }
        }
    }
    
    // For validate debug()
    private static String space = "                                                            ";
    private String indent( int level )
    {
        if( level < 1 )
            return( "***" );
        else
            return( space.substring( 0, level ) );
    }
    
    /*
    private void dumpValidate()
    {
        int level = 0;
        int i = 0;
        HtmlPart hp = null;
        while( i < alItems.size() )
        {
            hp = alItems.get( i++ );
            if( hp.type == HtmlPartType.TAG_START )
            {
                level++;
                System.out.println( indent( level ) + " start (" + i + ") " + hp.value + " " + hp.type );
            }
            else if( hp.type == HtmlPartType.TAG_END )
            {
                System.out.println( indent( level ) + " end   (" + i + ") " + hp.value + " " + hp.type );
                level--;
            }
        }
    }
    */
    
    private void validate()
    {
        //dumpValidate();
        
        HashMap< String, String > hmCurrentAttrs = new HashMap< String, String >();
        LinkedList< HtmlPart > llTagStack = new LinkedList< HtmlPart >();
        int level = 1;
        int i = 0;
        int j = 0;
        boolean firstTime = true;
        HtmlPart hp = null;
        while( i < alItems.size() )
        {
            hp = alItems.get( i );
            if( hp.type == HtmlPartType.TAG_START )
            {
                // make sure the first tag is "html" and case is correct for doctype declaration
                if( firstTime )
                {
                    if( hp.value.toLowerCase( Locale.ENGLISH ).equals( doctypeRootElement ) ) // "html"
                    {
                        doctypeRootElement = hp.value; // makes sure case is correct
                    }
                    else
                    {
                        // insert "html" start tag
                        HtmlPart hp2 = new HtmlPart();
                        hp2.value = doctypeRootElement; // "html"
                        hp2.type = HtmlPartType.TAG_START;
                        hp2.offset = -1;
                        alItems.add( i, hp2 );
                        
                        if( debugValidate )
                            System.out.println( "inserted html (" + i + ") " + hp2.value + " " + hp2.type + " " + hp2.level );
                        
                        continue;
                    }
                    firstTime = false;
                }
                else
                {
                    // fix any nested "html", change to "div" with "InvalidHtmlTag" attribute
                    if( hp.value.toLowerCase( Locale.ENGLISH ).equals( doctypeRootElement ) ) // "html"
                    {
                        hp.value = "div";
                        HtmlPart hp2 = new HtmlPart();
                        hp2.value = "InvalidHtmlTag";
                        hp2.type = HtmlPartType.ATTR_NAME;
                        hp2.offset = -1;
                        alItems.add( i + 1, hp2 );
                        HtmlPart hp3 = new HtmlPart();
                        hp3.value = doctypeRootElement;
                        hp3.type = HtmlPartType.ATTR_VALUE;
                        hp3.offset = -1;
                        alItems.add( i + 2, hp3 );
                    }
                }
                hmCurrentAttrs.clear();                

                // check if there's a "proper" matching end
                int countStart = 1;
                int countEnd = 0;
                int insertIndex = -1;
                for( j = i + 1; j < alItems.size(); j++ )
                {
                    HtmlPart hp2 = alItems.get( j );
                    if( hp2.value.toLowerCase( Locale.ENGLISH ).equals( hp.value.toLowerCase( Locale.ENGLISH ) ) &&
                        hp2.type == HtmlPartType.TAG_START )
                    {
                        if( insertIndex == -1 )
                            insertIndex = j;
                        countStart++;
                    }
                    if( hp2.value.toLowerCase( Locale.ENGLISH ).equals( hp.value.toLowerCase( Locale.ENGLISH ) ) &&
                        hp2.type == HtmlPartType.TAG_END )
                    {
                        if( insertIndex == -1 )
                            break;
                        countEnd++;
                    }
                }
                if( insertIndex != -1 )
                {
                    if( countStart > countEnd + 1 )
                    {
                        // insert end tag ("level" assigned later)
                        HtmlPart hp2 = new HtmlPart();
                        hp2.value = new String( hp.value );
                        hp2.type = HtmlPartType.TAG_END;
                        hp2.offset = -1;
                        alItems.add( insertIndex, hp2 );
                        
                        if( debugValidate )
                            System.out.println( "inserted end later (" + insertIndex + ") " + hp2.value + " " + hp2.type + " " + hp2.level );
                    }
                }
                
                hp.level = level++;
                llTagStack.push( hp );
                
                if( debugValidate )
                    System.out.println( indent( hp.level ) + " start (" + i + ") " + hp.value + " " + hp.type + " " + hp.level );
            }
            else if( hp.type == HtmlPartType.TAG_EMPTY )
            {                
                hmCurrentAttrs.clear();
                
                hp.level = level;
            }
            else if( hp.type == HtmlPartType.TAG_END )
            {
                if( llTagStack.size() == 0 )
                {
                    // remove end tag
                    alItems.remove( i );
                    
                    if( debugValidate )
                        System.out.println( "removed end (" + i + ") " + hp.value + " " + hp.type + " " + hp.level );
                    
                    continue;
                }
                else
                {
                    // Check to see if there's another "html" end tag, change current to "div"
                    if( hp.value.toLowerCase( Locale.ENGLISH ).equals( doctypeRootElement ) )
                    {                        
                        for( j = i + 1; j < alItems.size(); j++ )
                        {
                            HtmlPart hp2 = alItems.get( j );
                            if( hp2.value.toLowerCase( Locale.ENGLISH ).equals( doctypeRootElement ) &&
                                hp2.type == HtmlPartType.TAG_END )
                            {
                                hp.value = "div";
                                break;
                            }
                        }
                    }
                    
                    HtmlPart hp2 = llTagStack.pop();
                    
                    if( debugValidate )
                        System.out.println( "pop " + hp2.value + " " + hp2.type + " " + hp2.level );
                    
                    if( hp2.value.toLowerCase( Locale.ENGLISH ).equals( hp.value.toLowerCase( Locale.ENGLISH ) ) )
                    {
                        hp.level = --level;
                        hp.value = new String( hp2.value );
                        
                        if( debugValidate )
                            System.out.println( indent( hp.level ) + " end (" + i + ") " + hp.value + " " + hp.type + " " + hp.level );
                    }
                    else
                    {
                        // Check to see if current end tag has a match,
                        // either remove current or add missing
                        boolean found = false;
                        LinkedList< HtmlPart > llTagStack2 = new LinkedList< HtmlPart >();
                        while( llTagStack.size() > 0 )
                        {
                            HtmlPart hp3 = llTagStack.pop();
                            llTagStack2.push( hp3 );
                            if( hp3.value.toLowerCase( Locale.ENGLISH ).equals( hp.value.toLowerCase( Locale.ENGLISH ) ) )
                            {
                                found = true;
                                break;
                            }
                        }
                        while( llTagStack2.size() > 0 )
                            llTagStack.push( llTagStack2.pop() );
                        if( found )
                        {                        
                            // insert end tag
                            HtmlPart hp3 = new HtmlPart();
                            hp3.value = new String( hp2.value );
                            hp3.type = HtmlPartType.TAG_END;
                            hp3.offset = -1;
                            hp.level = --level;
                            alItems.add( i, hp3 );
                            
                            if( debugValidate )
                                System.out.println( "inserted end (" + i + ") " + hp3.value + " " + hp3.type + " " + hp3.level );
                            
                            llTagStack.push( hp2 );
                            continue;
                        }
                        else
                        {
                            llTagStack.push( hp2 );
                            
                            // remove end tag
                            alItems.remove( i );
                            
                            if( debugValidate )
                                System.out.println( "removed end (" + i + ") " + hp.value + " " + hp.type + " " + hp.level );
                            
                            continue;
                        }
                    }
                }
                
                // remove anything after the "html" end tag
                if( hp.value.toLowerCase( Locale.ENGLISH ).equals( doctypeRootElement ) )
                {
                    i++;
                    while( i < alItems.size() )
                        alItems.remove( i );
                    llTagStack.clear();
                    break;
                }
            }
            else if( hp.type == HtmlPartType.ATTR_NAME )
            {
                // XML does not allow duplicate attributes
                String name = hmCurrentAttrs.get( hp.value );
                if( name != null )
                {
                    // remove attribute name
                    alItems.remove( i );
                    // remove attribute value
                    alItems.remove( i );
                    
                    if( debugValidate )
                        System.out.println( "removed attribute " + hp.value + " " + hp.type );
                    
                    continue;
                }
                else
                {
                    hmCurrentAttrs.put( hp.value, hp.value );
                }
            }
            else if( hp.type == HtmlPartType.ATTR_SOLO )
            {
                // XML does not allow duplicate attributes
                String name = hmCurrentAttrs.get( hp.value );
                if( name != null )
                {
                    // remove attribute name
                    alItems.remove( i );
                    
                    if( debugValidate )
                        System.out.println( "removed attribute " + hp.value + " " + hp.type );
                    
                    continue;
                }
                else
                {
                    hmCurrentAttrs.put( hp.value, hp.value );
                }
            }
            i++;
        }
        
        // add end tags for any remaining start tags
        while( llTagStack.size() > 0 )
        {
            HtmlPart hp4 = llTagStack.pop();
            HtmlPart hp5 = new HtmlPart();
            hp5.value = new String( hp4.value );
            hp5.type = HtmlPartType.TAG_END;
            hp5.offset = -1;
            alItems.add( hp5 );
            
            if( debugValidate )
                System.out.println( "inserted end (append) " + hp5.value + " " + hp5.type + " " + hp5.level );
        }
    }
    
    private void process( boolean encodingOnly )
    {
        try
        {
            // record BOM if it exists
            if( bb.limit() >= 3 )
            {
                byte[] bytes = new byte[ 3 ];
                bb.get( bytes, 0, 3 );
                if( ( bytes[ 0 ] & 0xFF ) == 0xEF &&
                    ( bytes[ 1 ] & 0xFF ) == 0xBB &&
                    ( bytes[ 2 ] & 0xFF ) == 0xBF )
                    b = 3;
                else
                    bb.rewind();                    
            }
            
            // process
            parse( encodingOnly );
            if( configValidate || !encodingOnly )
                validate();
        }
        catch( Exception ex )
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            ex.printStackTrace( pw );
            alIssues.add( sw.toString() );
        }
    }
    
    private void parseFile( String file, boolean encodingOnly )
    {
        parseReset();
        
        File f = new File( file );
        if( !f.exists() )
        {
            alIssues.add( "File does not exist" );
            return;
        }
        if( f.length() == 0 )
        {
            alIssues.add( "File size is zero" );
            return;
        }
        if( f.length() > Integer.MAX_VALUE )
        {
            alIssues.add( "File size is greater than " + Integer.MAX_VALUE );
            return;
        }
        int fileSize = ( int )f.length();
        
        bb = ByteBuffer.allocate( fileSize );
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream( f );
            FileChannel fc = fis.getChannel();
            int read = fc.read( bb );
            if( read != fileSize )
            {
                alIssues.add( "Error reading file" );
                return;
            }
        }
        catch( Exception ex )
        {
            alIssues.add( "Exception: " + ex.getMessage() );
            return;
        }
        
        bb.position( 0 );
        
        process( encodingOnly );
    }
    
    /**
     * There are four methods to parse the HTML:
     * parseFile( String file ),
     * parseData( byte[] bytes ),
     * parseFileEncodingOnly( String file ),
     * parseDataEncodingOnly( byte[] bytes ).
     * <p>
     * This method parses the file completely, taking the file name
     * as an argument.
     * @param file File to be parsed
     */
    public void parseFile( String file )
    {
        parseFile( file, false );
    }
    
    /**
     * There are four methods to parse the HTML:
     * parseFile( String file ),
     * parseData( byte[] bytes ),
     * parseFileEncodingOnly( String file ),
     * parseDataEncodingOnly( byte[] bytes ).
     * <p>
     * This method parses the file until know the encoding, taking the file
     * name as an argument.
     * @param file File to be parsed
     */
    public String parseFileEncodingOnly( String file )
    {
        parseFile( file, true );
        return( encodingText );
    }
    
    private void parseData( byte[] bytes, boolean encodingOnly )
    {
        parseReset();
        
        if( bytes == null ||
            bytes.length == 0 )
        {
            alIssues.add( "Buffer is empty" );
            return;            
        }
        int size = bytes.length;
        
        bb = ByteBuffer.allocate( size );
        bb.put( bytes );
        
        bb.position( 0 );
        
        process( encodingOnly );
    }
    
    /**
     * There are four methods to parse the HTML:
     * parseFile( String file ),
     * parseData( byte[] bytes ),
     * parseFileEncodingOnly( String file ),
     * parseDataEncodingOnly( byte[] bytes ).
     * <p>
     * This method parses the file completely, taking a byte array
     * as an argument.
     * @param bytes Byte array to be parsed
     */
    public void parseData( byte[] bytes )
    {
        parseData( bytes, false );
    }
    
    /**
     * There are four methods to parse the HTML:
     * parseFile( String file ),
     * parseData( byte[] bytes ),
     * parseFileEncodingOnly( String file ),
     * parseDataEncodingOnly( byte[] bytes ).
     * <p>
     * This method parses the file until know the encoding, taking a byte
     * array as an argument.
     * @param bytes Byte array to be parsed
     */
    public String parseDataEncodingOnly( byte[] bytes )
    {
        parseData( bytes, true );
        return( encodingText );
    }
    
    /**
     * Returns the character set (encoding) used in the HTML file.
     * 
     * @return Character set (encoding) used in HTML file.
     */
    public String getEncoding()
    {
        return( encodingText );
    }
    
    /**
     * Returns the number of parse issues found.  Used to determine
     * if there were any errors during the parse.  A value of zero
     * means the HTML was successfully parsed.
     * 
     * @return The number of issues found during the parse.
     */
    public int getNumParseIssues()
    {
        return( alIssues.size() );        
    }

    private void writeParseIssuesToOutputStream( OutputStream os )
    {
        try
        {
            byte[] BOM = new byte[ 3 ];
            BOM[ 0 ] = ( byte )0xEF;
            BOM[ 1 ] = ( byte )0xBB;
            BOM[ 2 ] = ( byte )0xBF;
            os.write( BOM );
            Iterator< String > it = alIssues.iterator();
            while( it.hasNext() )
            {
                String s = it.next() + "\n";
                byte[] bytes = s.getBytes( encodingTags );
                os.write( bytes );
            }
            os.close();
        }
        catch( Exception ex ) {}
    }
    
    /**
     * Writes the parse issues to a given file.
     * 
     * @param file File in which to write parse issues.
     */
    public void writeParseIssuesToFile( String file )
    {
        try
        {
            FileOutputStream fos = new FileOutputStream( file );
            writeParseIssuesToOutputStream( fos );
        }
        catch( Exception ex ) {}
    }
    
    /**
     * Returns parse issues as a string.
     * 
     * @return The parse issues.
     */
    public String getParseIssues()
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeParseIssuesToOutputStream( baos );
            return( baos.toString( encodingTags ) );
        }
        catch( Exception ex )
        {
            return( "" );
        }
    }
    
    /**
     * Returns the number of parse items (tokens) found.  Used for
     * informational purposes only.
     * 
     * @return The number of items (tokens) found during the parse.
     */
    public int getNumParseItems()
    {
        return( alItems.size() );        
    }
       
    private void writeParseItemsToOutputStream( OutputStream os )
    {
        try
        {
            if( encodingText.equals( encodingTags ) ) // encodingTags = UTF-8
            {
                byte[] BOM = new byte[ 3 ];
                BOM[ 0 ] = ( byte )0xEF;
                BOM[ 1 ] = ( byte )0xBB;
                BOM[ 2 ] = ( byte )0xBF;
                os.write( BOM );
            }
            Iterator< HtmlPart > it = alItems.iterator();
            int colWidth = 20;
            while( it.hasNext() )
            {
                HtmlPart hp = it.next();
                String s0 = "offset=" + hp.offset;
                byte[] bytes = s0.getBytes( encodingText );
                os.write( bytes );
                for( int i = 0; i < colWidth - s0.length(); i++ )
                    os.write( ATTR_SPACE );
                String s1 = "type=" + hp.type;
                bytes = s1.getBytes( encodingText );
                os.write( bytes );
                for( int i = 0; i < colWidth - s1.length(); i++ )
                    os.write( ATTR_SPACE );
                String s2 = "level=" + hp.level;
                bytes = s2.getBytes( encodingText );
                os.write( bytes );
                for( int i = 0; i < colWidth - s2.length(); i++ )
                    os.write( ATTR_SPACE );
                String s3 = "value~" + hp.value + "~\n";
                bytes = s3.getBytes( encodingText );
                os.write( bytes );
            }
            os.close();
        }
        catch( Exception ex ) {}
    }
    
    /**
     * Writes the parse items (tokens) to a given file.
     * 
     * @param file File in which to write parse items (tokens).
     */
    public void writeParseItemsToFile( String file )
    {
        try
        {
            FileOutputStream fos = new FileOutputStream( file );
            writeParseItemsToOutputStream( fos );
        }
        catch( Exception ex ) {}
    }
    
    /**
     * Returns parse items (tokens) as a string.
     * 
     * @return The parse items (tokens).
     */
    public String getParseItems()
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeParseItemsToOutputStream( baos );
            return( baos.toString( encodingText ) );
        }
        catch( Exception ex )
        {
            return( "" );
        }
    }
    
    private void writeCleanXmlToOutputStream( OutputStream os )
    {
        try
        {
            if( encodingText.equals( encodingTags ) ) // encodingTags = UTF-8
            {
                byte[] BOM = new byte[ 3 ];
                BOM[ 0 ] = ( byte )0xEF;
                BOM[ 1 ] = ( byte )0xBB;
                BOM[ 2 ] = ( byte )0xBF;
                os.write( BOM );
            }
            
            Iterator< HtmlPart > it = alItems.iterator();
            boolean prevTagStart = false;
            boolean prevTagEmpty = false;
                       
            while( it.hasNext() )
            {
                HtmlPart hp = it.next();
                String s = "";
                if( !( hp.type == HtmlPartType.ATTR_NAME ||
                       hp.type == HtmlPartType.ATTR_VALUE ||
                       hp.type == HtmlPartType.ATTR_SOLO ) )
                {
                    if( prevTagStart )
                    {
                        s += ">";
                        prevTagStart = false;
                    }
                    if( prevTagEmpty )
                    {
                        s += "/>";
                        prevTagEmpty = false;
                    }
                }
                if( hp.type == HtmlPartType.TAG_START )
                {
                    prevTagStart = true;
                    if( configElemAttrLowerCase )
                        s += "<" + hp.value.toLowerCase( Locale.ENGLISH ); // open for attributes
                    else
                        s += "<" + hp.value; // open for attributes
                }   
                else if( hp.type == HtmlPartType.TAG_EMPTY )
                {
                    prevTagEmpty = true;
                    if( configElemAttrLowerCase )
                        s += "<" + hp.value.toLowerCase( Locale.ENGLISH ); // open for attributes
                    else
                        s += "<" + hp.value; // open for attributes
                }
                else if( hp.type == HtmlPartType.TAG_END )
                {
                    if( configElemAttrLowerCase )
                        s += "</" + hp.value.toLowerCase( Locale.ENGLISH ) + ">";
                    else
                        s += "</" + hp.value + ">";
                }
                else if( hp.type == HtmlPartType.TAG_DECL )
                {
                    // Other DOCTYPEs cause issue with Java XML Parser
                    // Must be the first element (erase anything before it)
                    // As for the other declarations, commenting them out for now (TODO)
                    if( hp.value.toUpperCase().startsWith( "DOCTYPE" ) )
                        s = "<!DOCTYPE " + doctypeRootElement + ">";
                    else
                        s += "<!-- " + hp.value + " -->";
                }
                else if( hp.type == HtmlPartType.TAG_DECL2 )
                {
                    s += "<![" + hp.value + "]]>";
                }
                else if( hp.type == HtmlPartType.TAG_PI )
                {
                    s += "<?" + hp.value + "?>";
                }
                else if( hp.type == HtmlPartType.TAG_COMMENT )
                {
                    s += "<!--" + hp.value + "-->";
                }
                else if( hp.type == HtmlPartType.ATTR_NAME )
                {
                    if( configElemAttrLowerCase )
                        s += " " + hp.value.toLowerCase( Locale.ENGLISH ) + "="; // open for attribute value
                    else
                        s += " " + hp.value + "="; // open for attribute value
                }
                else if( hp.type == HtmlPartType.ATTR_VALUE )
                {
                    int index = hp.value.indexOf( ATTR_QUOTE2 );
                    if( index == -1 )
                        s += ( char )ATTR_QUOTE2 + hp.value + ( char )ATTR_QUOTE2;
                    else
                        s += ( char )ATTR_QUOTE1 + hp.value + ( char )ATTR_QUOTE1;
                }
                else if( hp.type == HtmlPartType.ATTR_SOLO )
                {
                    s += " " + hp.value + "=" + "\"" + hp.value + "\"";
                }
                else if( hp.type == HtmlPartType.TEXT )
                {
                    s += hp.value;
                }
                else if( hp.type == HtmlPartType.TEXT_SCRIPT )
                {
                    if( hp.value.length() > 0 &&
                        hp.value.toUpperCase( Locale.ENGLISH ).indexOf( "CDATA" ) == -1 )
                        s += "//<![CDATA[" + hp.value + "//]]>";
                    else
                        s += hp.value;
                }
                else if( hp.type == HtmlPartType.TEXT_STYLE )
                {
                    s += hp.value;
                }
                byte[] bytes = s.getBytes( encodingText );
                os.write( bytes );
            }
            if( prevTagStart )
            {
                String s = ">";
                byte[] bytes = s.getBytes( encodingText );
                os.write( bytes );
            }
            if( prevTagEmpty )
            {
                String s = "/>";
                byte[] bytes = s.getBytes( encodingText );
                os.write( bytes );                
            }
            os.close();
        }
        catch( Exception ex ) {}
    }
    
    /**
     * Writes the clean XML to given file.
     * 
     * @param file File in which to write the clean XML.
     */
    public void writeCleanXmlToFile( String file )
    {
        try
        {
            FileOutputStream fos = new FileOutputStream( file );
            writeCleanXmlToOutputStream( fos );
        }
        catch( Exception ex ) {}
    }
    
    /**
     * Returns the clean XML as a string.
     * 
     * @return The clean XML.
     */
    public String getCleanXml()
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writeCleanXmlToOutputStream( baos );
            return( baos.toString( encodingText ) );
        }
        catch( Exception ex )
        {
            return( "" );
        }
    }
}