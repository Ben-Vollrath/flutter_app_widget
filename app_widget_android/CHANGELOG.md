## 0.4.0
* breaking changes: `configureWidget` and `updateWidget` accept `layoutId` instead of `layoutName`
* breaking changes: `onConfigureWidget` now accept 3 params (`widgetId`, `layoutId`, `layoutName`)

## 0.3.3
* fix: fix trigger update widget updating the widget multiple time

## 0.3.2

* chore: update latest dependencies

## 0.3.1

* chore: use latest platform interface
## 0.3.0

*feat: support widget provider with different package name
*fix: fix MainActivity not using application package name

## 0.2.1

* fix(android): onClickWidget callback should be independent on each widget

## 0.2.0

* breaking changes: `androidAppName` renamed to `androidPackageName`
* breaking changes: parameter `textViewsIdMap` renamed to `textViews` for `configureWidget` and `updateWidget`
* breaking changes: `onClickWidget` now accept String instead of Map
* feat: androidPackageName are now accepted for reloadWidget and getWidgetIds
* feat: accept uri parameters in `configureWidget` and `updatewidget`
* fix: onClickWidget callback

## 0.1.1

* perf: improve configure widget callback response
* fix: fix reload wigdets
* fix: fix getWidgetIds

## 0.1.0

* feat: support flavored app
* fix: update method
* chore: finalized android params name
## 0.0.4

* implement `handleConfigureAction` android method

## 0.0.3

* implement getWidgetIds api

## 0.0.2

* implement updateWidget api
* remove debug log - Android and Dart

## 0.0.1

* implement configureWidget api
* implement cancelConfifureWidget api
* implement reloadWidgets api
* implement widgetExist api
* implement onConfigureWidget callback api
* implement onClickWidget callback api
