mkdir build
cd build
rm librender.dll
cmake .. -G "MinGW Makefiles"
cmake --build .
copy librender.dll ..\..\..\build\librender.dll