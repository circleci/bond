var page = require('webpage').create();
var system = require('system');

if (system.args.length !== 2) {
  console.log('Expected a target URL parameter.');
  phantom.exit(1);
}

page.onConsoleMessage = function (message) {
  console.log(message);
};

var url = system.args[1];

page.open(url, function (status) {
  if (status !== "success") {
    phantom.exit(1);
  }

  var exitCode = page.evaluate(function() {
    bond.test.test_runner.run_tests();
    return window["exit-code"];
  });

  phantom.exit(exitCode);
});
