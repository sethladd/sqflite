import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:sqflite/sqflite.dart';
import 'package:sqflite_example/model/main_item.dart';
import 'package:sqflite_example/open_test_page.dart';
import 'package:sqflite_example/simple_test_page.dart';
import 'package:sqflite_example/src/main_item_widget.dart';

void main() {
  runApp(new MyApp());
}

class MyApp extends StatefulWidget {
  // This widget is the root of your application.

  @override
  _MyAppState createState() => new _MyAppState();
}

const String testSimpleRoute = "/test/simple";
const String testOpenRoute = "/test/open";

class _MyAppState extends State<MyApp> {
  var routes = <String, WidgetBuilder>{
    '/test': (BuildContext context) => new MyHomePage(),
    testSimpleRoute: (BuildContext context) => new SimpleTestPage(),
    testOpenRoute: (BuildContext context) => new OpenTestPage()
  };
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
        title: 'Sqflite Demo',
        theme: new ThemeData(
          // This is the theme of your application.
          //
          // Try running your application with "flutter run". You'll see
          // the application has a blue toolbar. Then, without quitting
          // the app, try changing the primarySwatch below to Colors.green
          // and then invoke "hot reload" (press "r" in the console where
          // you ran "flutter run", or press Run > Hot Reload App in IntelliJ).
          // Notice that the counter didn't reset back to zero -- the application
          // is not restarted.
          primarySwatch: Colors.blue,
        ),
        home: new MyHomePage(title: 'Sqflite Demo Home Page'),
        routes: routes);
  }
}

class MyHomePage extends StatefulWidget {
  List<MainItem> items = [];

  MyHomePage({Key key, this.title}) : super(key: key) {
    items.add(new MainItem("Simple tests", "Basic SQLite operations",
        route: testSimpleRoute));
    items.add(new MainItem("Open tests", "Open onCreate/onUpgrade/onDowngrade",
        route: testOpenRoute));
  }

  // This widget is the home page of your application. It is stateful,
  // meaning that it has a State object (defined below) that contains
  // fields that affect how it looks.

  // This class is the configuration for the state. It holds the
  // values (in this case the title) provided by the parent (in this
  // case the App widget) and used by the build method of the State.
  // Fields in a Widget subclass are always marked "final".

  final String title;

  @override
  _MyHomePageState createState() => new _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _platformVersion = 'Unknown';

  get _itemCount => widget.items.length;

  @override
  initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await Sqflite.platformVersion;
    } on PlatformException {
      platformVersion = "Failed to get platform version";
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
        appBar: new AppBar(
          title: new Text('Sqflite demo'),
        ),
        body: new ListView.builder(
            itemBuilder: _itemBuilder, itemCount: _itemCount));
  }

  //new Center(child: new Text('Running on: $_platformVersion\n')),

  Widget _itemBuilder(BuildContext context, int index) {
    return new MainItemWidget(widget.items[index], (MainItem item) {
      Navigator.of(context).pushNamed(item.route);
    });
  }
}
