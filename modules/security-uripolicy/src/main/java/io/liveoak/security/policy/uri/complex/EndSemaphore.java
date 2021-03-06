/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.security.policy.uri.complex;

/**
 * Helper class to check if drools rules processing should be finished
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class EndSemaphore {

    private boolean finished = false;

    public boolean isFinished() {
        return finished;
    }

    public void setFinished( boolean finished ) {
        this.finished = finished;
    }
}
