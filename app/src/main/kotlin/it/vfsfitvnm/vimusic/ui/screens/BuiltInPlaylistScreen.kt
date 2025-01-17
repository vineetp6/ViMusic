package it.vfsfitvnm.vimusic.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.route.RouteHandler
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.BuiltInPlaylist
import it.vfsfitvnm.vimusic.models.DetailedSong
import it.vfsfitvnm.vimusic.ui.components.LocalMenuState
import it.vfsfitvnm.vimusic.ui.components.TopAppBar
import it.vfsfitvnm.vimusic.ui.components.themed.*
import it.vfsfitvnm.vimusic.ui.styling.LocalColorPalette
import it.vfsfitvnm.vimusic.ui.styling.LocalTypography
import it.vfsfitvnm.vimusic.ui.views.SongItem
import it.vfsfitvnm.vimusic.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map


@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistScreen(
    builtInPlaylist: BuiltInPlaylist,
) {
    val lazyListState = rememberLazyListState()

    val albumRoute = rememberAlbumRoute()
    val artistRoute = rememberArtistRoute()

    RouteHandler(listenToGlobalEmitter = true) {
        albumRoute { browseId ->
            AlbumScreen(
                browseId = browseId ?: error("browseId cannot be null")
            )
        }

        artistRoute { browseId ->
            ArtistScreen(
                browseId = browseId ?: error("browseId cannot be null")
            )
        }

        host {
            val density = LocalDensity.current
            val menuState = LocalMenuState.current

            val binder = LocalPlayerServiceBinder.current
            val colorPalette = LocalColorPalette.current
            val typography = LocalTypography.current

            val thumbnailSize = remember {
                density.run {
                    54.dp.roundToPx()
                }
            }

            val songs by remember(binder?.cache, builtInPlaylist) {
                when (builtInPlaylist) {
                    BuiltInPlaylist.Favorites -> Database.favorites()
                    BuiltInPlaylist.Cached -> Database.songsByRowIdDesc().map { songs ->
                        songs.filter { song ->
                            song.song.contentLength?.let { contentLength ->
                                binder?.cache?.isCached(song.song.id, 0, contentLength)
                            } ?: false
                        }
                    }
                }
            }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 64.dp),
                modifier = Modifier
                    .background(colorPalette.background)
                    .fillMaxSize()
            ) {
                item {
                    TopAppBar(
                        modifier = Modifier
                            .height(52.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.chevron_back),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier
                                .clickable(onClick = pop)
                                .padding(vertical = 8.dp, horizontal = 16.dp)
                                .size(24.dp)
                        )

                        Image(
                            painter = painterResource(R.drawable.ellipsis_horizontal),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier
                                .clickable {
                                    menuState.display {
                                        Menu {
                                            MenuCloseButton(onClick = menuState::hide)

                                            MenuEntry(
                                                icon = R.drawable.time,
                                                text = "Enqueue",
                                                enabled = songs.isNotEmpty(),
                                                onClick = {
                                                    menuState.hide()
                                                    binder?.player?.enqueue(songs.map(DetailedSong::asMediaItem))
                                                }
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .size(24.dp)
                        )
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            BasicText(
                                text = when (builtInPlaylist) {
                                    BuiltInPlaylist.Favorites -> "Favorites"
                                    BuiltInPlaylist.Cached -> "Cached"
                                },
                                style = typography.m.semiBold
                            )

                            BasicText(
                                text = "${songs.size} songs",
                                style = typography.xxs.semiBold.secondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.text),
                                modifier = Modifier
                                    .clickable {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayFromBeginning(
                                            songs
                                                .map(DetailedSong::asMediaItem)
                                                .shuffled()
                                        )
                                    }
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .background(
                                        color = colorPalette.elevatedBackground,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .size(20.dp)
                            )

                            Image(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(colorPalette.text),
                                modifier = Modifier
                                    .clickable {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayFromBeginning(
                                            songs.map(
                                                DetailedSong::asMediaItem
                                            )
                                        )
                                    }
                                    .shadow(elevation = 2.dp, shape = CircleShape)
                                    .background(
                                        color = colorPalette.elevatedBackground,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.song.id },
                    contentType = { _, song -> song },
                ) { index, song ->
                    SongItem(
                        song = song,
                        thumbnailSize = thumbnailSize,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(songs.map(DetailedSong::asMediaItem), index)
                        },
                        menuContent = {
                            when (builtInPlaylist) {
                                BuiltInPlaylist.Favorites -> InFavoritesMediaItemMenu(song = song)
                                BuiltInPlaylist.Cached -> NonQueuedMediaItemMenu(mediaItem = song.asMediaItem)
                            }
                        }
                    )
                }
            }
        }
    }
}
