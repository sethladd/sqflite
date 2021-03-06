import 'dart:io';
import 'package:flutter/widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart';
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';
import 'package:sqflite/src/utils.dart';
import 'package:sqflite_example/model/item.dart';
import 'package:sqflite_example/model/test.dart';
import 'package:sqflite_example/src/item_widget.dart';
import 'dart:async';

class TestPage extends StatefulWidget {
  // return the path
  Future<String> initDeleteDb(String dbName) async {
    Directory documentsDirectory = await getApplicationDocumentsDirectory();
    print(documentsDirectory);

    String path = join(documentsDirectory.path, dbName);
    await deleteDatabase(path);
    return path;
  }

  String title;
  List<Test> tests = [];
  test(String name, Func0<FutureOr> fn) {
    tests.add(new Test(name, fn));
  }

  TestPage(this.title) {}

  @override
  _TestPageState createState() => new _TestPageState();
}

class _TestPageState extends State<TestPage> {
  int get _itemCount => items.length;

  List<Item> items = [];

  _run() async {
    if (!mounted) {
      return null;
    }

    setState(() {
      items.clear();
    });

    for (Test test in widget.tests) {
      Item item = new Item("${test.name}");
      int position;
      setState(() {
        position = items.length;
        items.add(item);
      });
      try {
        await test.fn();

        item = new Item("${test.name}")..state = ItemState.success;
      } catch (e) {
        print(e);
        item = new Item("${test.name}")..state = ItemState.failure;
      }

      if (!mounted) {
        return null;
      }

      setState(() {
        items[position] = item;
      });
    }
  }

  @override
  initState() {
    super.initState();
    /*
    setState(() {
      _itemCount = 3;
    });
    */
    _run();
  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
        appBar: new AppBar(title: new Text(widget.title), actions: <Widget>[
          new IconButton(
            icon: new Icon(Icons.refresh),
            tooltip: 'Run again',
            onPressed: _run,
          ),
        ]),
        body: new ListView.builder(
            itemBuilder: _itemBuilder, itemCount: _itemCount));
  }

  Widget _itemBuilder(BuildContext context, int index) {
    Item item = getItem(index);
    return new ItemWidget(item);
  }

  Item getItem(int index) {
    return items[index];
  }
}
