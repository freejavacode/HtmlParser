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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * Wrapper for Java's {@link Charset} class.  It's main role is to return the
 * proper {@link Charset} for a given "character set string" (utilizing just
 * alphanumeric characters to determine a match).  It will also convert
 * strings from one character set to another.  The motivation comes from the
 * erroneous character set strings found in HTML.
 * 
 * @author Free Java Code
 *
 */
public class JavaCharsets
{
    private SortedMap< String, Charset > smCharsets;
    private TreeMap< String, String > tmStrippedKeys;
    
    /**
     * Initializes lookup tables for this class.
     */
    public JavaCharsets()
    {
        smCharsets = Charset.availableCharsets();
        Set< String > sCharsetKeys = smCharsets.keySet();
        tmStrippedKeys = new TreeMap< String, String >();
        Iterator< String > it = sCharsetKeys.iterator();
        while( it.hasNext() )
        {
            String key = it.next();
            String keyLower = key.toLowerCase();
            String keyStripped = "";
            for( int i = 0; i < keyLower.length(); i++ )
            {
                if( Character.isLetterOrDigit( keyLower.charAt( i ) ) )
                    keyStripped += keyLower.charAt( i );
            }   
            tmStrippedKeys.put( keyStripped, keyLower );
        }       
    }
    
    /**
     * Returns the proper {@link Charset} for a given "character set string"
     * (utilizing just alphanumeric characters to determine a match).
     * 
     * @param charset given character set string
     * @return resultant {@link Charset}
     */
    public Charset getCharset( String charset )
    {
        String charsetLower = charset.toLowerCase();
        String charsetStripped = "";
        for( int i = 0; i < charsetLower.length(); i++ )
        {
            if( Character.isLetterOrDigit( charsetLower.charAt( i ) ) )
                charsetStripped += charsetLower.charAt( i );
        }
        
        Iterator< String > it = tmStrippedKeys.descendingKeySet().iterator();
        while( it.hasNext() )
        {
            String keyStripped = it.next();
            if( keyStripped.equals(  charsetStripped.subSequence( 0, charsetStripped.length() ) ) )
                return( smCharsets.get( tmStrippedKeys.get( keyStripped ) ) );
        }
        return( null );
    }
    
    /**
     * Converts given source string into given destination character set.
     * 
     * @param srcString source string
     * @param destCharset destination character set
     * @return new string converted to new character set
     */
    public String convertString( String srcString, String destCharset )
    {
        try
        {
            Charset destCS = getCharset( destCharset );
            ByteBuffer bb = destCS.encode( srcString );
            String s = new String( bb.array(), destCharset );
            return( s.trim() );
        }
        catch( Exception ex )
        {
            return( "" );
        }
    }
}
