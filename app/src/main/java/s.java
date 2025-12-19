public class s import 'dart:convert';
        import 'dart:math';
        import 'package:flutter/material.dart';
        import 'package:flutter/services.dart';
        import 'package:flutter_svg/svg.dart';
        import 'package:flutter_switch/flutter_switch.dart';
        import 'package:fluttertoast/fluttertoast.dart';
        import 'package:google_fonts/google_fonts.dart';
        import 'package:shared_preferences/shared_preferences.dart';
        import '../Devices_Structure/Device_Structure.dart';
        import '../MQTT_STRUCTURE/MQTT_SETUP.dart';
        import '../Main_Screens/Map_Base_Screen/Map_Control_Operations.dart';

class GroupSlider extends StatefulWidget {
    final List<String> deviceIds;
    final MQTTService mqttService;
    final MapInteractionController mapController;
    final DraggableScrollableController scrollController;
    final bool technicianMode;
    final bool isGroupSelectionEnabled;

  const GroupSlider({
        super.key,
                required this.deviceIds,
                required this.mqttService,
                required this.mapController,
                required this.scrollController,
                required this.technicianMode,
                required this.isGroupSelectionEnabled,
    });


    @override
    State<GroupSlider> createState() => _GroupSliderState();
}

class _GroupSliderState extends State<GroupSlider> {

    static const Map<String, Map<String, dynamic>> deviceMapping =   {
            "t": {
        "icon": "assets/Filter_Icons/Turkish_lamp.svg",
                "width": 40.0,
                "height": 40.0,
    },
            "sp": {
        "icon": "assets/Filter_Icons/Spike.svg",
                "width": 40.0,
                "height": 40.0,
    },
            "st": {
        "icon": "assets/Filter_Icons/Led_Strip.svg",
                "width": 20.0,
                "height": 20.0,
    },
            "r": {
        "icon": "assets/Filter_Icons/Recessed_Light.svg",
                "width": 25.0,
                "height": 25.0,
    },
            "f": {
        "icon": "assets/Filter_Icons/Fairy_light.svg",
                "width": 25.0,
                "height": 25.0,
    },
            "c": {
        "icon": "assets/Filter_Icons/Chandelier.svg",
                "width": 25.0,
                "height": 25.0,
    },
};

static const Map<String, String> groupTitleMapping = {
        // Ground Floor (GF)
        "G0": "Grand Wall Stairs",
        "G1": "Side Linear Rail",
        "G2": "Center Liner Rail",

        "G3": "Fountain Outer",
        "G4": "Fountain Inner",
        "G5": "Fountain Flower Strip",

        "G6": "Fountain Pool Light",
        "G7": "Fountain Flower Light",

        "G8": "Corridor Pendants",
        "G9": "Pillar's Side",
        "G10": "Pillar's Front",
        "G11": "Pillar's Center",
        "G12": "Outdoor Eave Strip",
        "G13": "Halls Spike Light",

        "G14": "Entrance Stairs Strip",

        "G15": "Entrance Arch",
        "G16": "Entrance Spike Light",

        "G20": "Mazar Strip",

        // First Floor (1F)
        "G17": "---",
        "G18": "Corridor Pendants",
        "G19": "Pillar's Side",
        "G21": "Stair Chandelier",
        };


final List<DeviceModel> _selectedDevices = [];
late final List<String> deviceIds;

double _brightness = 128;
bool _isOn = true;
bool _loading = true;
bool _sceneEnabled = false;
late FToast fToast;


@override
void initState() {
    super.initState();
    fToast = FToast();
    fToast.init(context);
    _loadDevicesData();
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
}

@override
void didUpdateWidget(covariant GroupSlider oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.deviceIds != widget.deviceIds) {
        _loadDevicesData();
    }
}

Future<void> _loadDevicesData() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getStringList("devices") ?? [];

    _selectedDevices.clear();

    for (final deviceStr in stored) {
        final device = DeviceModel.fromJson(jsonDecode(deviceStr));
        if (widget.deviceIds.contains(device.deviceId)) {
            _selectedDevices.add(device);
        }
    }

    // 🔹 Check only the currently running scene
    bool sceneEnabled = true;
    final keys = prefs.getKeys().where((key) => key.startsWith('device_state_scene_'));

    for (final key in keys) {
        final isOn = prefs.getBool(key) ?? false;
        if (isOn) {
            sceneEnabled = true;
            break; // found a running scene, enable slider
        } else {
            sceneEnabled = false;
        }
    }

    if (mounted) {
        bool anyOn = _selectedDevices.any((d) => d.isOn);
        double avgBrightness = _selectedDevices.isNotEmpty
                ? _selectedDevices.map((d) => d.brightness).reduce((a, b) => a + b) /
        _selectedDevices.length
          : 128;

        setState(() {
            _isOn = anyOn;
            _brightness = avgBrightness;
            _sceneEnabled = sceneEnabled; // disable only if no scene is ON
            _loading = false;
        });
    }
}

bool get _isOnOffOnlyGroup {
    if (_selectedDevices.isEmpty) return false;
    final groupId = _selectedDevices.first.deviceId.split("_").first;
    return groupId == "G6" || groupId == "G7";
}


Future<void> _saveDevicesState(bool isOn, double brightness) async {
    final prefs = await SharedPreferences.getInstance();
    final deviceList = prefs.getStringList('devices') ?? [];

    final updatedList =
    deviceList.map((deviceStr) {
    final json = jsonDecode(deviceStr);
    if (widget.deviceIds.contains(json['deviceId'])) {
        json['isOn'] = isOn;
        json['brightness'] = brightness.round();
    }
    return jsonEncode(json);
    }).toList();

    await prefs.setStringList('devices', updatedList);

    for (var deviceId in widget.deviceIds) {
        await prefs.setBool('${deviceId}_isOn', isOn);
        await prefs.setInt('${deviceId}_brightness', brightness.round());
    }
}

void _updateBrightness(double value) async {
    if (_isOnOffOnlyGroup) return;


    final newBrightness = value.round().clamp(0, 255); // allow 0
    setState(() => _brightness = newBrightness.toDouble());

    final newIsOn = newBrightness > 0;
    setState(() => _isOn = newIsOn);

    await _saveDevicesState(newIsOn, _brightness);

    widget.mapController.updateDeviceStateFromExternal(
            widget.deviceIds,
            newIsOn,
            );

    if (!widget.mqttService.isConnected) return;

    for (final device in _selectedDevices) {
        final elementIds = device.element.split(',');
        for (final id in elementIds) {
            final message =
            newIsOn
                    ? '#*2*${id.trim()}*2*${_brightness.round()}*#'
                    : '#*2*${id.trim()}*2*0*#';

            widget.mqttService.publish(device.publishTopic, message);
        }
    }
}

void _toggleSwitch(bool value) async {
    if (!_sceneEnabled) {
        _showSceneToast();
        return;
    }

    setState(() => _isOn = value);

    final double brightnessToSend =
            _isOnOffOnlyGroup ? (_isOn ? 255 : 0) : _brightness;

    await _saveDevicesState(_isOn, brightnessToSend);

    widget.mapController.updateDeviceStateFromExternal(
            widget.deviceIds,
            _isOn,
            );

    if (!widget.mqttService.isConnected) return;

    for (final device in _selectedDevices) {
        final elementIds = device.element.split(',');

        for (final id in elementIds) {
            final message = _isOn
                    ? '#*2*${id.trim()}*2*${brightnessToSend.round()}*#'
                    : '#*2*${id.trim()}*2*0*#';

            widget.mqttService.publish(device.publishTopic, message);
        }
    }
}


void _showSceneToast() {
    final toast = Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
    decoration: BoxDecoration(
            color: const Color(0xFF171717),
            borderRadius: BorderRadius.circular(16),
      ),
    child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
    SvgPicture.asset(
            'assets/bottom_Icons/Error_Active_Scene.svg',
            width: 35,
            height: 35,
          ),
          const SizedBox(width: 10),
          const Text(
            'Select a scene to control devices',
            style: TextStyle(
            color: Color(0xFFD9D9D9),
            fontSize: 17,
            fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );

    fToast.showToast(
            child: toast,
            gravity: ToastGravity.CENTER,
            toastDuration: const Duration(seconds: 2),
    );
}



@override
Widget build(BuildContext context) {
    if (_loading) {
        return const SizedBox.shrink();
    }
    final double screenWidth = MediaQuery.of(context).size.width;

    return  Padding(
            padding: const EdgeInsets.only(top: 14,right: 4),
    child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
    _iconAndLabel(),
          const SizedBox(height: 30),
    _buildBrightnessControl(screenWidth),



    if (widget.technicianMode)
        Padding(
                padding: const EdgeInsets.only(top: 30),
    child: _buildDetailsPanel(),
            ),
        ],
      ),
    );
}

Widget _iconAndLabel() {
    if (_selectedDevices.isEmpty) return const SizedBox();

    final device = _selectedDevices.first;
    final String titleText = widget.isGroupSelectionEnabled
            ? groupTitleMapping[device.deviceId] ??
            groupTitleMapping[
                    device.deviceId.split("_").first
                    ] ??
                    "Group"
                    : device.location;


    return Padding(
            padding: const EdgeInsets.only(left: 5, top: 4),
    child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
    Row(
            mainAxisSize: MainAxisSize.min,
            children: [
    Flexible(
            fit: FlexFit.loose,
            child: Text(
            titleText,
            overflow: TextOverflow.ellipsis,
            style: GoogleFonts.nunito(
            color: Color(0xFFD9D9D9),
            fontSize: 20,
            fontWeight: FontWeight.w700,
                  ),
                ),
              ),

              const SizedBox(width: 10),

    GestureDetector(
            onTap: () {
        if (widget.scrollController.isAttached) {
            widget.scrollController.animateTo(
                    1.0,
                    duration: const Duration(milliseconds: 320),
            curve: Curves.easeOut,
                    );
        }
    },
    child: (widget.deviceIds.length > 1)
            ? SvgPicture.asset(
            "assets/bottom_Icons/GroupCheck.svg",
            height: 18,
            colorFilter: const ColorFilter.mode(Color(0xFFD9D9D9), BlendMode.srcIn),
                )
                    : SvgPicture.asset(
            // "assets/bottom_Icons/Info.svg",
            "assets/bottom_Icons/GroupCheck.svg",
            height: 18,
            colorFilter: const ColorFilter.mode(Color(0xFFD9D9D9), BlendMode.srcIn),
                ),
              ),
            ],
          ),

    // Right side Switch
    FlutterSwitch(
            width: 50.0,
            height: 29.0,
            toggleSize: 28.0,
            value: _isOn,
            borderRadius: 20.0,
            padding: 0.4,
            activeColor: const Color(0xFF00A1F1),
            inactiveColor: Color(0xFF666666),
            activeToggleColor: Color(0xFFFFFFFF),
            inactiveToggleColor: Color(0xFFD9D9D9),
            onToggle: (value) async => _toggleSwitch(value),
          ),
        ],
      ),
    );
}

Widget _buildBrightnessControl(double screenWidth) {
    return Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
    // 🔹 Device Icon
    if (_selectedDevices.isNotEmpty)
        Builder(builder: (context) {
    final device = _selectedDevices.first;

    final parts = device.deviceId.split("_");
    String typeCode = "";
    if (parts.length > 1) {
        final mid = parts[1];
        typeCode = mid.replaceAll(RegExp(r'[0-9]'), '');
    }

    final iconPath = deviceMapping[typeCode]?['icon'];
    final iconWidth = deviceMapping[typeCode]?['width'] ?? 30.0;
    final iconHeight = deviceMapping[typeCode]?['height'] ?? 30.0;

    return Padding(
            padding: const EdgeInsets.only(right: 30.0, left: 10),
    child: SizedBox(
            width: 50,
            height: 45,
            child: iconPath != null
            ? SvgPicture.asset(
            iconPath,
            width: iconWidth > 40 ? 40 : iconWidth,
            height: iconHeight > 40 ? 40 : iconHeight,
            fit: BoxFit.contain,
            colorFilter: ColorFilter.mode(
            _isOn
                    ? const Color(0xFFFFBB00)
                        : const Color(0xFF666666),
            BlendMode.srcIn,
                  ),
                )
                    : const Icon(
            Icons.lightbulb_outline_rounded,
            color: Color(0xFFFFBB00),
            size: 40,
                ),
              ),
            );
          }),

    // 🔹 Brightness Slider
    Expanded(
            child: Opacity(
            opacity: _isOn ? 1.0 : 0.35,
            child: ClipRRect(
            borderRadius: BorderRadius.circular(15),
            child: Container(
            height: 55,
            decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(15),
            gradient: const LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
    Color(0xFF000000),
            Color(0xFF151515),
            Color(0xFF262626),
            Color(0xFF303030),
                    ],
                  ),
                ),
    child: Stack(
            children: [
    // 🔹 Fill bar
    LayoutBuilder(
            builder: (context, constraints) {
        final fullWidth = constraints.maxWidth;

        final effectiveBrightness = _isOnOffOnlyGroup
                ? (_isOn ? 255 : 0)
                : max(_brightness, 1);

        final double fillWidth = max(
                (effectiveBrightness / 255) * fullWidth,
                effectiveBrightness > 0 ? 8.0 : 0.0,
                ).toDouble();


        return AnimatedContainer(
                duration: const Duration(milliseconds: 200),
        width: fillWidth,
                height: double.infinity,
                decoration: BoxDecoration(
                color: _isOn
                ? const Color(0xFFFFBB00)
                                : const Color(0xFF666666),
                borderRadius: BorderRadius.horizontal(
                left: const Radius.circular(16),
                right: fillWidth >= fullWidth
                ? const Radius.circular(16)
                                  : Radius.zero,
                            ),
                          ),
                        );
    },
                    ),

    // 🔹 Percentage Text (FIXED 1–100%)
    Align(
            alignment: Alignment.centerRight,
            child: Padding(
            padding: const EdgeInsets.only(right: 20),
    child: Text(
            _isOnOffOnlyGroup
                    ? (_isOn ? '100%' : '0%')
                    : '${max(1, ((_brightness / 255) * 100).round())}%',
            style: GoogleFonts.nunito(
            color:
    _brightness > 220 ? Colors.black : Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ),

    // 🔹 Slider
    SliderTheme(
            data: SliderTheme.of(context).copyWith(
            inactiveTrackColor: Colors.transparent,
            activeTrackColor: Colors.transparent,
            thumbColor: Colors.transparent,
            overlayColor: Colors.transparent,
            thumbShape:
                        const RoundSliderThumbShape(enabledThumbRadius: 0),
    overlayShape: SliderComponentShape.noOverlay,
                      ),
    child: Slider(
            min: 1,
            max: 255,
            value: _isOnOffOnlyGroup
            ? (_isOn ? 255 : 1)
            : max(1, _brightness).toDouble(),

            onChanged: (value) {
    if (!_sceneEnabled) {
        _showSceneToast();
        return;
    }

    // 🔥 G6 / G7 → ON / OFF
    if (_isOnOffOnlyGroup) {
        if (value > 128 && !_isOn) {
            _toggleSwitch(true);
        } else if (value <= 128 && _isOn) {
            _toggleSwitch(false);
        }
        return;
    }

    // 🔹 Normal groups → 1–255 only
    if (!_isOn) _toggleSwitch(true);

    setState(() {
        _brightness = value.round().clamp(1, 255).toDouble();
    });
                        },

    onChangeEnd: (value) {
    if (!_sceneEnabled || _isOnOffOnlyGroup) return;
    _updateBrightness(value.clamp(1, 255));
                        },
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
}


Widget _buildDetailsPanel() {
    if (_selectedDevices.isEmpty) return const SizedBox.shrink();

    final device = _selectedDevices.first;

    // Map dynamic details
    final Map<String, String> deviceDetails = {
            "Device ID": device.deviceId,
            "Device Name": "Light 1",
            "Device Type": device.deviceType,
            "Location": device.location,
            "Firmware Version":"--",
            "Hardware Version": "--",
            "Part Of Group":"--",
            "Power": "Mains",
    };

    return Container(
            height: 280,
            width: double.infinity,
            padding:  EdgeInsets.only(left: 5),
    decoration: const BoxDecoration(
            color: Colors.transparent,
      ),
    child: Padding(
            padding: const EdgeInsets.only(left: 10, right: 5, top: 20),
    child: ListView.separated(
            physics: const NeverScrollableScrollPhysics(),
            itemCount: deviceDetails.length,
            separatorBuilder: (_, __) => const SizedBox(height: 10),
    itemBuilder: (context, index) {
        final label = deviceDetails.keys.elementAt(index);
        final value = deviceDetails.values.elementAt(index);
        return Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
        SizedBox(
                width: 170,
                child: Text(
                "$label:",
                style: GoogleFonts.nunito(
                color: const Color(0xFFB3B3B3),
                fontSize: 17,
                fontWeight: FontWeight.w600,
                    ),
                  ),
                ),

        Expanded(
                child: Text(
                value,
                softWrap: true,
                overflow: TextOverflow.visible,
                style: GoogleFonts.nunito(
                color: const Color(0xFF666666),
                fontSize: 17,
                fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            );
    },
        ),
      ),
    );
}
}{
}
