/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import java.io.IOException;
import java.io.Writer;

public class IndentingWriter2 extends Writer {
    private final StringBuilder writer;
    private final char[] buffer = new char[16];
    private int indentLevel = 0;
    private boolean beginningOfLine;
    private static final String newLine = System.getProperty("line.separator");

    public IndentingWriter2() {
        this.writer = new StringBuilder(32);
    }


    public IndentingWriter2(StringBuilder buf) {
        //buf not null
        this.writer = buf;
    }


    @Override
    public void write(int chr) throws IOException {
        //synchronized(lock) {
            if (chr == '\n') {
                writer.append(newLine);
                beginningOfLine = true;
            } else {
                if (beginningOfLine) {
                    for (int i=0; i<indentLevel; i++) {
                       writer.append(' ');
                    }
                }
                beginningOfLine = false;
                writer.append((char)chr);
            }
        //}
    }

    @Override
    public void write(char[] chars) throws IOException {
        //synchronized(lock) {
            for (char chr: chars) {
                write(chr);
            }
        //}
    }

    @Override
    public void write(char[] chars, int start, int len) throws IOException {
        //synchronized(lock) {
            len = start+len;
            while (start < len) {
                write(chars[start++]);
            }
        //}
    }

	public String getLastLine()
	{
		String[] s =writer.toString().split("\n");
		for(int i = s.length-1;i>0;i-=1)
		{
			if(!s[i].replace(" ","").equals(""))
				return s[i];
		}
		return "";
		
	}
    @Override
    public void write(String s) throws IOException {
        //synchronized (lock) {
            for (int i=0,len=s.length(); i<len; i++) {
                write(s.charAt(i));
            }
        //}
    }

    @Override
    public void write(String str, int start, int len) throws IOException {
        //synchronized(lock) {
            len = start+len;
            while (start < len) {
                write(str.charAt(start++));
            }
        //}
    }

    @Override
    public Writer append(CharSequence charSequence) throws IOException {
        write(charSequence.toString());
        return this;
    }

    @Override
    public Writer append(CharSequence charSequence, int start, int len) throws IOException {
        write(charSequence.subSequence(start, len).toString());
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        write(c);
        return this;
    }

    @Override
    public void flush() {
        //synchronized(lock) {
     //       writer.flush();
        //}
    }

    @Override
    public void close() {
        //synchronized(lock) {
       //     writer.close();
        //}
    }

    public StringBuilder getStringBuilder(){
        return writer;
    }

    public String getString(){
        return writer.toString();
    }

    public void indent(int indentAmount) {
        //synchronized(lock) {
            this.indentLevel += indentAmount;
            if (indentLevel < 0) {
                indentLevel = 0;
            }
        //}
    }

    public void deindent(int indentAmount) {
        //synchronized(lock) {
            this.indentLevel -= indentAmount;
            if (indentLevel < 0) {
                indentLevel = 0;
            }
        //}
    }

    public void printLongAsHex(long value) throws IOException {
        int bufferIndex = 0;
        do {
            int digit = (int)(value & 15);
            if (digit < 10) {
                buffer[bufferIndex++] = (char)(digit + '0');
            } else {
                buffer[bufferIndex++] = (char)((digit - 10) + 'a');
            }

            value >>>= 4;
        } while (value != 0);

        while (bufferIndex>0) {
            write(buffer[--bufferIndex]);
        }
    }

    public void printIntAsDec(int value) throws IOException {
        int bufferIndex = 0;
        boolean negative = value < 0;

        do {
            int digit = value % 10;
            buffer[bufferIndex++] = (char)(digit + '0');

            value = value / 10;
        } while (value != 0);

        if (negative) {
            write('-');
        }

        while (bufferIndex>0) {
            write(buffer[--bufferIndex]);
        }
    }
}
