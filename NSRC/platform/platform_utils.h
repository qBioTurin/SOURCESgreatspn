#ifndef __PLATFORM_UTILS_H__
#define __PLATFORM_UTILS_H__

#ifdef __cplusplus
extern "C" {
#endif

//=============================================================================
// Utilities to simplify cross-platform portability
//=============================================================================

// return the vector of environment variables
char* const* get_environ();

// open a file with the default viewer
// filename doesn't need to be quoted
// return 0 on success
int open_file(const char* filename);

// spawn a new process exec_name with the given arguments
// file arguments don't need to be quoted
// return 0 on success
int execp_cmd(const char* exec_name, const char* const* args, int verbose);

// convert a dot file into a pdf file using Graphviz.
// file arguments don't need to be quoted
// return 0 on success
int dot_to_pdf(const char *dot_fname, const char *pdf_fname);

// convert a eps file into a pdf file, using ghostscript.
// need to have the bounding box size, in points.
// return 0 on success
int eps_to_pdf_bbox(const char *eps_fname, const char *pdf_fname, int width, int height);

//=============================================================================

// is the tool being called from the new Java-based GUI?
// returns 1 if true
int invoked_from_gui();

// if it is running from the portable app-image directory,
// returns the directory name, otherwise returns NULL
const char* get_appimage_dir();

// search for the path where the GreatSPN subcommand @bin can be found
// returns 0 if the binary is found
int search_greatspn_app(char cmdname[FILENAME_MAX], const char *bin);

// generate a temporary filename, using either the default or the extra TEMP dir
int portable_mkstemp(char buffer[FILENAME_MAX], const char* pattern);

//=============================================================================

#if defined(WIN32) || defined(_WIN32) || defined(__CYGWIN__)
# define PATH_SEPARATOR      "\\" 
# define PATH_SEPARATOR_CH   '\\' 
# define EXE_SUFFIX          ".exe"
#else 
# define PATH_SEPARATOR      "/" 
# define PATH_SEPARATOR_CH   '/' 
# define EXE_SUFFIX          ""
#endif 

//=============================================================================

#ifdef __cplusplus
} // extern "C"
#endif

#endif // __PLATFORM_UTILS_H__