package unittesting.activities;

import unittesting.proxies.ENUM_UnitTestResult;

public class AssertActivity extends ExecutionActivity {
    private final String name;
    private final boolean result;
    private final String message;

    public AssertActivity(int sequence, String name, boolean result, String failureMessage) {
        super(sequence);
        this.name = name;
        this.result = result;
        this.message = result ? null : failureMessage;
    }

    public String getName() {
        return this.name;
    }

    public boolean isSuccess() {
        return this.result;
    }

    public boolean isFailed() {
        return !this.result;
    }

    public ENUM_UnitTestResult getResultEnum() {
        return this.result ? ENUM_UnitTestResult._3_Success : ENUM_UnitTestResult._2_Failed;
    }

    public String getMessage() {
        return this.message;
    }
}
