package unittesting.activities;

public class ExceptionExecutionActivity extends ExecutionActivity {
    private final Exception exception;

    public ExceptionExecutionActivity(int sequence, Exception exception) {
        super(sequence);
        this.exception = exception;
    }

    public Exception getException() {
        return this.exception;
    }
}
