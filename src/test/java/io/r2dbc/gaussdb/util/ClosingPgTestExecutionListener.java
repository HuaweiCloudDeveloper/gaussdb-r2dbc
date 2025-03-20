package io.r2dbc.gaussdb.util;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class ClosingPgTestExecutionListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (GaussDBServerExtension.containerInstance != null) {
            GaussDBServerExtension.containerInstance.stop();
        }
        if (GaussDBServerExtension.containerNetwork != null) {
            GaussDBServerExtension.containerNetwork.close();
        }
    }

}
