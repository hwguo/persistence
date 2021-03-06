/**
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.persistence.util.common;

/**
 * Container.
 * 
 * @param <T>
 *            type of the elements
 * @author Fabiel Zuniga
 * @author Nachiket Abhyankar
 */
public interface Container<T> {

    /**
     * Verifies whether the given element is contained.
     * 
     * @param element
     *            element to verify
     * @return {@code true} if {@code element} is contained, {@code false} otherwise
     */
    boolean contains(T element);
}
