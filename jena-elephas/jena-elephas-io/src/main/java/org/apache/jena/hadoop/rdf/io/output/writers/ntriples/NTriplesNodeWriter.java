/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.hadoop.rdf.io.output.writers.ntriples;

import java.io.Writer;

import org.apache.jena.atlas.lib.CharSpace ;
import org.apache.jena.hadoop.rdf.io.output.writers.AbstractNodeWriter;
import org.apache.jena.riot.out.NodeFormatterNT;

/**
 * A NTriples based node writer
 * 
 * 
 * 
 * @param <TValue>
 *            Value type
 */
public class NTriplesNodeWriter<TValue> extends AbstractNodeWriter<TValue> {

    /**
     * Creates a new writer
     * 
     * @param writer
     *            Writer
     */
    public NTriplesNodeWriter(Writer writer) {
        super(writer);
    }

    /**
     * Creates a new writer
     * 
     * @param writer
     *            Writer
     * @param charSpace
     *            Character space to use
     */
    public NTriplesNodeWriter(Writer writer, CharSpace charSpace) {
        super(writer, new NodeFormatterNT(charSpace));
    }

}
