package unittesting.activities;

import unittesting.proxies.ENUM_UnitTestResult;

public class StartEndExecutionActivity extends ExecutionActivity {
    private final boolean result;
    private final String message;

    public StartEndExecutionActivity(int sequence, boolean result, String message) {
        super(sequence);
        this.result = result;
        this.message = message;
    }

    public boolean getResult() {
        return this.result;
    }

    public ENUM_UnitTestResult getResultEnum() {
        return this.result ? ENUM_UnitTestResult._3_Success : ENUM_UnitTestResult._2_Failed;
    }

    public String getMessage() {
        return this.message;
    }
}
