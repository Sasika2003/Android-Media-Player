# Android-Media-Player                      
A comprehensive Android media player application built with Kotlin that demonstrates advanced audio and video playback capabilities with real-time effects processing.                   

## Features                                
### 🎵 Audio Playback                                                  
- **Multiple Audio Sources**: Play default sample audio or select custom audio files                               
- **Real-time Audio Effects**:                                 
  - Bass Boost enhancement                             
  - Reverb effects (Large Hall preset)                             
  - Equalizer presets (Pop and Rock)                 
  - Clean playback (no effects)                               
                              
### 🎬 Video Playback                            
- **Flexible Video Sources**: Support for default sample videos and custom video files                          
- **Real-time Video Effects**:                             
  - Grayscale filter                            
  - Sepia tone effect                         
  - Color inversion                                   
  - High contrast enhancement                             
  - Standard playback (no effects)                     
- **Advanced Video Controls**: Integrated MediaController with play, pause, seek functionality                       
- **Hardware-accelerated rendering** using TextureView                               
                           
### 📷 Camera Integration                            
- **Video Recording**: Direct video capture using device camera                   
- **Image Capture**: Take photos and display them within the app                        
- **Permission Management**: Automatic camera permission handling                  
                   
## Technical Implementation                 
                          
### Core Technologies                
- **Language**: Kotlin                  
- **UI Framework**: Android Views with AppCompatActivity                          
- **Media Framework**: Android MediaPlayer API                       
- **Video Rendering**: TextureView with SurfaceTexture                  
- **Effects Processing**:               
  - Audio: BassBoost, PresetReverb, Equalizer                 
  - Video: ColorMatrix and ColorMatrixColorFilter                 
                                                    
## Installation & Setup                 
                                        
1. Clone the repository                                                
git clone https://github.com/Sasika2003/Android-Media-Player.git                    
2. Open the project in Android Studio                              
3. Ensure you have the following in your `res/raw/` directory:                     
   - `sample_audio` (audio file for default playback)                   
   - `sample_video` (video file for default playback)                         
4. Build and run the application                           

## Permissions Required                 
                   
```xml
<uses-permission android:name="android.permission.CAMERA" />                       
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />                   
```                 
