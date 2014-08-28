UnitTesting
===========

Module to run Mendix and JUnit unit test inside a project.

## Dependencies

- Mendix 4.1.1 or higher
- Community Commons module

## Installation

- Import the module in your project (from the Mendix AppStore or by downloading and exporting the module from this project)
- Download the latest Community Commons module in your project
- Map the module role 'TestRunner' to the applicable user roles in your application
- Add the 'UnitTestOverview' microflow to your navigation structure
- [Optional for remote unit test run API] Add the 'Startup' flow to your models startup sequence
- [Optional for remote unit test run API] Set the constants `UnitTesting.RemoteApiEnabled` to `true` and provide a password to `UnitTesting.RemoteApiPassword`
- [Optional for remote unit test run API] When hosting in a cloud node or on premise; open a request handler on the `unittests/` path

## Usage 

Running unittests is quite trivial. First navigate to the 'Test Suite Overview'. On the top of this form there is an overview of testsuites. A testsuite reflects all unittests that are available in a module of your project. When selecting a testsuite, all unit tests inside the test suite are displayed at the bottom of the screen, including their last result if applicable. 

The `Run all unit tests` button finds and runs all unit tests in the selected test suite. When unit tests are running, the progress will be tracked in the grid below the button. 

The `Rerun selected test (class)` runs the unit test currently selected. If a JUnit test class is selected, all tests in the test class are run another time. 

The `View result` button displays additional details about the unit test result, such as the last step and exception stacktraces. 

As example, try running all unit tests in the UnitTesting module. Those should be available by default. 

## Creating Unit Tests


### Creating a microflow unit tests
 
To create a new microflow test in a module, just add a microflow with a name that starts with (case sensitive) "Test". A test microflow should have no input arguments and either no result type, a boolean result or a string result. For string results, a non empty string is interpreted as error message. A microflow without return type is considered to be successful as long as no exceptions where thrown. 
 
Furthermore it is possible to create a Setup and TearDown microflow per module. Those microflows are invoked once before and after eacht test run (regardless whether the test run consists on one or multiple unit tests).  
 
The UnitTesting module publishes a reportStep microflow that can be used inside your test microflow to track progress inside a test. The last step that was successfully reached in a unit test is reported back in the test result. This makes it easier to inspect where things go wrong. (Albeit using the microflow debugger is usually more insightful)

### Creating a Java unit tests (with JUNit)
 
The Java unit test runner is driven by junit.org and requires general understanding of junit (version 4). A Junit test method is run if it exists somewhere in the module namespace (that is, it is stored a java class that lives somewhere in the folder javasource/yourmodulename). A java function is recognized as test if it is public, non static, parameterless and annotated with the org.junit.Test annotation. Multiple tests can exists in a single class, but junit does not guarantuee the execution order of the tests.  

See the files `src/javasource/unittesting/UnittestingUnitTest1.java` and `src/javasource/unittesting/UnittestingUnitTest2.java` for some example JUnit unit tests. 

The AbstractUnitTest class can be used to base unit test classes on. This class provides some time measurement functions (to ignore setup / teardown  in the time measure) and the reportStep function. (Which is otherwise accessible through TestManager.instance().reportStep). 

## Running unit tests through the remote api

Running unit tests through the remote json REST api is pretty trivial. A new test run can be kicked off (if none is running yet) by using the endpoint `unittests/start`. Example:

```json
POST http://localhost:8080/unittests/start
{
	"password" : "1"
}
```

This request will be responded to with a `204 NO CONTENT` response if the test run was started successfully. From that point on, one can pull for the status of the test run by invoking `unittests/status`. Example:

```json
POST http://localhost:8080/unittests/status

{
	"password" : "1"
}
```

Example response:
```json
{
    "failures": 1,
    "tests": 10,
    "runtime": -1,
    "failed_tests": [{
        "error": "This exception is doopery nice",
        "name": "MyFirstModule.Test_ThisTestIsSupposedToFailToCheckExceptionRendering",
        "step": "Starting microflow test 'MyFirstModule.Test_ThisTestIsSupposedToFailToCheckExceptionRendering'"
    }],
    "completed": false
}
```

Please note that the `completed` flag will be false as long as the test run isn't finished. The `runtime` flag will return the total runtime of the suite in milliseconds after the test run has finished.