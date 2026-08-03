def log = new File(basedir, 'build.log').text

assert log.contains('1 type(s) were not analysed') : 'what was left out should be counted: ' + log
return true
