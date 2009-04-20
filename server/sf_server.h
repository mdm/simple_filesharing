void handle_sigint(int signal);

struct filelist_entry
{
	int size;
	char name[1001]; /* unlimited length of filenames is a security risk. Filesystems usually impose a limit of 256 bytes or characters */
};

struct client
{
	int socket;
	char socket_addr[16];
	int socket_port;
	char status; /* 'd' == disconnected, 'c' == connected, 'r' == registered, 's' == sending file list */
	char line_buffer[1025];
	int download_port;
	struct filelist_entry *file_list;
	int filelist_size; /* # of elements allocated */
	int filelist_len; /* # of elements in use */
} *clients;

