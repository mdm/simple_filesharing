#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>

#include <sys/socket.h>
/*#include <sys/types.h>/**/
#include <netinet/in.h>
/*#include <unistd.h>/**/

#include "sf_server.h"

#define UDP_PORT 4950
#define BACKLOG 10


int join_multicast_group(int udp_socket, const char* group_address)
{
	struct ip_mreq multicast_request;

	if (inet_aton(group_address, &(multicast_request.imr_multiaddr.s_addr)) == -1)
	{
		perror("Could not convert IP address from string.\n");
		return -1;
	}
	multicast_request.imr_interface.s_addr = INADDR_ANY;

	return setsockopt(udp_socket, IPPROTO_IP, IP_ADD_MEMBERSHIP, &multicast_request, sizeof(struct ip_mreq));
}

int leave_multicast_group(int udp_socket, const char* group_address)
{
	struct ip_mreq multicast_request;

	if (inet_aton(group_address, &(multicast_request.imr_multiaddr.s_addr)) == -1)
	{
		fprintf(stderr, "Could not convert IP address from string.\n");
		return -1;
	}
	multicast_request.imr_interface.s_addr = INADDR_ANY;

	return setsockopt(udp_socket, IPPROTO_IP, IP_DROP_MEMBERSHIP, &multicast_request, sizeof(struct ip_mreq));
}

int udp_setup(int udp_port)
{
	int udp_socket;
	struct sockaddr_in server_udp_addr;

	/* create UDP socket */
	if ((udp_socket = socket(PF_INET, SOCK_DGRAM, 0)) != -1)
	{
		/* bind UDP socket to port x */
		memset(&server_udp_addr, 0, sizeof(server_udp_addr));
		server_udp_addr.sin_family = AF_INET;
		server_udp_addr.sin_port = htons(udp_port);
		server_udp_addr.sin_addr.s_addr = INADDR_ANY;

		if (bind(udp_socket, (struct sockaddr*)&server_udp_addr, sizeof(struct sockaddr)) == -1)
		{
			fprintf(stderr, "Could not bind socket.\n");
			close(udp_socket);
			return -1;
		}
			
		if (join_multicast_group(udp_socket, "224.0.0.42") == -1)
		{
			fprintf(stderr, "Could not join multicast group.\n");
		}
	}
	else
	{
		fprintf(stderr, "Could not create socket.\n");
	}

	return udp_socket;
}

int tcp_setup(int* tcp_port)
{
	int tcp_socket;
	struct sockaddr_in server_tcp_addr;
	int len;
	int yes = 1;

	/* create TCP socket */
	if ((tcp_socket = socket(PF_INET, SOCK_STREAM, 0)) != -1)
	{
		if (setsockopt(tcp_socket, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int)) == -1)
		{
			fprintf(stderr, "Could not make socket adress reusable.\n");
			close(tcp_socket);
			return -1;
		}

		/* bind UDP socket to port x */
		memset(&server_tcp_addr, 0, sizeof(server_tcp_addr));
		server_tcp_addr.sin_family = AF_INET;
		server_tcp_addr.sin_port = htons(0);
		server_tcp_addr.sin_addr.s_addr = INADDR_ANY;

		if (bind(tcp_socket, (struct sockaddr*)&server_tcp_addr, sizeof(struct sockaddr)) == -1)
		{
			fprintf(stderr, "Could not bind socket.\n");
			close(tcp_socket);
			return -1;
		}
		
		len = sizeof(server_tcp_addr);
		if (getsockname(tcp_socket, (struct sockaddr*)&server_tcp_addr, &len) == -1)
		{
			fprintf(stderr, "Could not read socket name.\n");
			close(tcp_socket);
			return -1;
		}
		*tcp_port = ntohs(server_tcp_addr.sin_port);

		if (listen(tcp_socket, BACKLOG) == -1)
		{
			fprintf(stderr, "Cannot listen on socket.\n");
			close(tcp_socket);
			return -1;
		}
	}
	return tcp_socket;
}

int push_global_filelist(struct client *clients, int num_clients)
{
	int i, j, k, num_files;
	char buffer[1024];

	num_files = 0;

	for (i = 0; i < num_clients; i++)
	{
		if (clients[i].status != 'd')
		{
			num_files = num_files + clients[i].filelist_len;
		}
	}

	for (i = 0; i < num_clients; i++)
	{
		if ((clients[i].status != 'd') && (clients[i].status != 's')) /* currently sending clients will not receive the update to avoid deadlocks */
		{
			sprintf(buffer, "update_filelist %d\n", num_files);
			send(clients[i].socket, buffer, strlen(buffer), 0);
			for (j = 0; j < num_clients; j++)
			{
				if (clients[j].status != 'd')
				{
					for (k = 0; k < clients[j].filelist_len; k++)
					{
						sprintf(buffer, "%d %s %d %s\n", clients[j].file_list[k].size, clients[j].socket_addr, clients[j].download_port, clients[j].file_list[k].name);
						send(clients[i].socket, buffer, strlen(buffer), 0);
					}
				}
			}
		}
	}
}

int main(int argc, char **argv)
{
	int udp_socket;
	int tcp_socket;
	int tcp_port;

	int client_socket;

	struct sockaddr_in addr;
	int addr_size;

	char buffer[1025];

	struct client *clients;

	int num_clients;
	int max_clients;

	int i, j;
	int len;
	char* temp_str;

	fd_set read_fds_master;
	fd_set read_fds_copy;
	int fd_max;

	FD_ZERO(&read_fds_master);
	FD_ZERO(&read_fds_copy);

	clients = malloc(BACKLOG * sizeof(struct client));
	num_clients = 0;
	max_clients = BACKLOG;

	
	/* signal(SIGINT, handle_sigint); */


	if ((udp_socket = udp_setup(UDP_PORT)) == -1) exit(1);

	/* make udp socket "select()able" */
	FD_SET(udp_socket, &read_fds_master);
	fd_max = udp_socket;

	if((tcp_socket = tcp_setup(&tcp_port)) == -1) exit(1);
	printf("Listening on tcp port: %d\n", tcp_port);

	/* make tcp socket "select()able" */
	FD_SET(tcp_socket, &read_fds_master);
	fd_max = tcp_socket;


	/* main loop */
	for (;;)
	{
		/* handle readable sockets */
		read_fds_copy = read_fds_master; // make a copy that select() can modify
		if (select(fd_max + 1, &read_fds_copy, NULL, NULL, NULL) == -1)
		{
			fprintf(stderr, "Error during select().\n");
			close(udp_socket);
			exit(1);
		}

		for (i = 0; i <= fd_max; i++)
		{
			if (FD_ISSET(i, &read_fds_copy))
			{
				if (i == udp_socket)
				{
					/* answer a multicast message */
					memset(buffer, 0, sizeof(buffer));
					addr_size = sizeof(addr);
					if (recvfrom(udp_socket, buffer, sizeof(buffer), 0, (struct sockaddr*)&addr, &addr_size) == -1)
					{
						fprintf(stderr, "Error receiving multicast message.\n");
						close(udp_socket);
						exit(1);
					}


					if (strncmp(buffer, "server_discovery", 16) == 0)
					{
						printf("%s:%d - discovery request\n", inet_ntoa(addr.sin_addr.s_addr), ntohs(addr.sin_port));
						len = sprintf(buffer, "discovery_reply %d\n", tcp_port);
						addr_size = sizeof(addr);
						if (sendto(udp_socket, buffer, len, 0, (struct sockaddr*)&addr, addr_size) == -1)
						{
							perror("Error answering multicast message.\n");
							close(udp_socket);
							exit(1);
						}
					}
				}
				if (i == tcp_socket)
				{
					for (j = 0; j < num_clients; j++)
					{
						if (clients[j].status == 'd') break; /* the value of j at this point is used as a side effect later */
					}
					/* j is now ALWAYS the first unused index of the clients array */
					if (j == num_clients) num_clients++;

					if (num_clients > max_clients)
					{
						max_clients = max_clients + BACKLOG;
						clients = realloc(clients, max_clients * sizeof(struct client));
					}
					addr_size = sizeof(addr);
					clients[j].socket = accept(tcp_socket, (struct sockaddr*)&addr, &addr_size);
					clients[j].status = 'c';

					/* save source as string for logging and debugging */
					strncpy(clients[j].socket_addr, (const char*)inet_ntoa(addr.sin_addr.s_addr), 15);
					clients[j].socket_port = ntohs(addr.sin_port);
					printf("%s:%d - new connection.\n", clients[j].socket_addr, clients[j].socket_port);
					FD_SET(clients[j].socket, &read_fds_master);
					if (clients[j].socket > fd_max) fd_max = clients[j].socket;
				}
				for (j = 0; j < num_clients; j++)
				{
					if (i == clients[j].socket)
					{
						memset(buffer, 0, sizeof(buffer));
						len = recv(clients[j].socket, buffer, sizeof(buffer) - 1 - strlen(clients[j].line_buffer), 0);
						switch (len)
						{
						case 0:
							printf("%s:%d - disconnected.\n", clients[j].socket_addr, clients[j].socket_port);
							FD_CLR(clients[j].socket, &read_fds_master);
							clients[j].status = 'd';
							push_global_filelist(clients, num_clients);
							break;
						case -1:
							/* TODO: handle gracefully. this is usually a "connection reset by peer" */
							perror("recv");
							break;
						default:
							strncat(clients[j].line_buffer, buffer, len);

							while (strchr(clients[j].line_buffer, '\n') != NULL)
							{
								switch (clients[j].status)
								{
								case 'c':
									//printf("status: connected\n");
									if (sscanf(clients[j].line_buffer, "register %d", &(clients[j].download_port)) == 1)
									{
										printf("%s:%d - registered port %d for downloads.\n", clients[j].socket_addr, clients[j].socket_port, clients[j].download_port);
										clients[j].status = 'r';
										break;
									}
									printf("%s:%d - sent %d unrecognized bytes: %s\n", clients[j].socket_addr, clients[j].socket_port, strlen(clients[j].line_buffer), clients[j].line_buffer);
									break;
								case 'r':
									//printf("status: registered\n");
									//printf("buffer: %s", clients[j].line_buffer);
									if (sscanf(clients[j].line_buffer, "send_filelist %d\n", &(clients[j].filelist_size)) == 1)
									{
										if (clients[j].filelist_size > 0)
										{
											printf("%s:%d - will send %d file list entries next.\n", clients[j].socket_addr, clients[j].socket_port, clients[j].filelist_size);
											if (clients[j].file_list != NULL) free(clients[j].file_list);
											clients[j].file_list = malloc(clients[j].filelist_size * sizeof(struct filelist_entry));
										}

										clients[j].status = 's';
										clients[j].filelist_len = 0;
									break;
									}
									if (strncmp(clients[j].line_buffer, "unregister", 10) == 0)
									{
										printf("%s:%d - unregistered download port.\n", clients[j].socket_addr, clients[j].socket_port);
										clients[j].status = 'c';
										break;
									}
									printf("%s:%d - sent %d unrecognized bytes: %s\n", clients[j].socket_addr, clients[j].socket_port, strlen(clients[j].line_buffer), clients[j].line_buffer);
									break;
								case 's':
									//printf("status: sending file list\n");
									if (clients[j].filelist_len < clients[j].filelist_size)
									{	
										len = clients[j].filelist_len;
										if (sscanf(clients[j].line_buffer , "%d %1000s", &(clients[j].file_list[len].size), clients[j].file_list[len].name) == 2)
										{
											clients[j].filelist_len = clients[j].filelist_len + 1;
											printf("%s:%d - sent a file list entry: %s (%d bytes)\n", clients[j].socket_addr, clients[j].socket_port, clients[j].file_list[len].name, clients[j].file_list[len].size);

										}
										else
										{
											printf("%s:%d - sent %d unrecognized bytes: %s\n", clients[j].socket_addr, clients[j].socket_port, strlen(clients[j].line_buffer), clients[j].line_buffer);
										}
									}
									if (clients[j].filelist_len == clients[j].filelist_size)
									{
										clients[j].status = 'r';
										push_global_filelist(clients, num_clients);
									}
									break;
								default:
									break;
								}

								/* consume part of buffer */
								temp_str = strchr(clients[j].line_buffer, '\n');
								//printf("bytes left: %d\n", strlen(temp_str));
								if (strlen(temp_str) > 1)
								{
									memset(buffer, 0, sizeof(buffer));
									strncpy(buffer, temp_str + 1, strlen(temp_str) - 1);
									//printf("bytes left: %d\n", strlen(buffer));
									memset(clients[j].line_buffer, 0, sizeof(clients[j].line_buffer));
									strncpy(clients[j].line_buffer, buffer, strlen(buffer));
									//printf("line buffer: %s", clients[j].line_buffer);
								}
								else
								{
									memset(clients[j].line_buffer, 0, sizeof(clients[j].line_buffer));
								}
							}
						}
					}
				}
			}
		}
	}
}

void handle_sigint(int signal)
{
	printf("Caught a SIGINT (Ctrl+C).\nBye!\n");
}

