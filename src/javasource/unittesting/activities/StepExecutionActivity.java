package unittesting.activities;

public class StepExecutionActivity extends ExecutionActivity {
    private final String message;

    public StepExecutionActivity(int sequence, String message) {
        super(sequence);
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}

